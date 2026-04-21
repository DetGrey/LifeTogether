package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOf

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.data.local.source.GuideLocalDataSource
import com.example.lifetogether.data.local.source.GuideProgressLocalDataSource
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.data.remote.GuideFirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideProgressState
import com.example.lifetogether.domain.repository.GuideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

class GuideRepositoryImpl @Inject constructor(
    private val guideLocalDataSource: GuideLocalDataSource,
    private val guideProgressLocalDataSource: GuideProgressLocalDataSource,
    private val guideFirestoreDataSource: GuideFirestoreDataSource,
) : GuideRepository {

    private companion object {
        const val TAG = "GuideRepositoryImpl"
        const val MIN_UPLOAD_INTERVAL_MS = 2 * 60 * 1000L
    }

    override fun observeGuides(familyId: String, uid: String): Flow<Result<List<Guide>, AppError>> {
        return guideLocalDataSource.getItems(familyId, uid)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                }
            }
    }

    override fun syncGuidesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>> = flow {
        coroutineScope {
            launch {
                guideFirestoreDataSource.guideProgressSnapshotListener(familyId, uid).collect { progressResult ->
                    when (progressResult) {
                        is Result.Success -> runCatching {
                            guideProgressLocalDataSource.updateGuideProgressFromRemote(
                                familyId = familyId,
                                uid = uid,
                                items = progressResult.data,
                            )
                        }.onFailure { error ->
                            Log.e(TAG, "Guide progress local update failure: ${error.message}", error)
                        }

                        is Result.Failure -> {
                            Log.e(TAG, "guide progress listener failure: ${progressResult.error}")
                        }
                    }
                }
            }

            var lastSharedGuides: List<Guide> = emptyList()
            var lastPrivateGuides: List<Guide> = emptyList()
            var sharedHasSuccessfulSync = false
            var privateHasSuccessfulSync = false

            combine(
                guideFirestoreDataSource.familySharedGuidesSnapshotListener(familyId),
                guideFirestoreDataSource.privateGuidesSnapshotListener(familyId, uid),
            ) { sharedResult, privateResult ->
                sharedResult to privateResult
            }.collect { (sharedResult, privateResult) ->
                val sharedGuides: List<Guide> = when (sharedResult) {
                    is Result.Success -> {
                        sharedHasSuccessfulSync = true
                        sharedResult.data.items.also { lastSharedGuides = it }
                    }

                    is Result.Failure -> {
                        Log.e(TAG, "shared listener failure: ${sharedResult.error}")
                        lastSharedGuides
                    }
                }

                val privateGuides: List<Guide> = when (privateResult) {
                    is Result.Success -> {
                        privateHasSuccessfulSync = true
                        privateResult.data.items.also { lastPrivateGuides = it }
                    }

                    is Result.Failure -> {
                        Log.e(TAG, "private listener failure: ${privateResult.error}")
                        lastPrivateGuides
                    }
                }

                val hadAnySuccessInThisEmission = sharedResult is Result.Success || privateResult is Result.Success
                val hasAnySuccessfulSync = sharedHasSuccessfulSync || privateHasSuccessfulSync
                if (!hadAnySuccessInThisEmission && !hasAnySuccessfulSync && sharedGuides.isEmpty() && privateGuides.isEmpty()) {
                    Log.w(TAG, "both guides listeners failed and no cached fallback exists; skipping local update")
                    emit(Result.Failure(AppErrors.storage("Guide listeners failed with no cached fallback")))
                    return@collect
                }

                val mergedGuides = (sharedGuides + privateGuides)
                    .associateBy { it.id ?: "" }
                    .values
                    .filter { !it.id.isNullOrBlank() }
                val hasFullSnapshotCoverage = sharedHasSuccessfulSync && privateHasSuccessfulSync

                runCatching {
                    if (mergedGuides.isEmpty()) {
                        if (hasFullSnapshotCoverage) {
                            guideLocalDataSource.deleteFamilyGuides(familyId)
                        }
                    } else {
                        if (hasFullSnapshotCoverage) {
                            guideLocalDataSource.updateGuides(mergedGuides.toList())
                        } else {
                            guideLocalDataSource.upsertGuides(mergedGuides.toList())
                        }
                    }
                }.onSuccess {
                    if (hasAnySuccessfulSync) {
                        emit(Result.Success(Unit))
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Guide local sync failure: ${error.message}", error)
                    emit(Result.Failure(AppErrors.fromThrowable(error)))
                }
            }
        }
    }

    override fun observeGuideById(familyId: String, id: String, uid: String): Flow<Result<Guide, AppError>> {
        return guideLocalDataSource.getItemById(familyId, id, uid)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveGuide(guide: Guide): Result<String, AppError> {
        return guideFirestoreDataSource.saveGuide(guide)
    }

    override suspend fun updateGuide(guide: Guide): Result<Unit, AppError> {
        return guideFirestoreDataSource.updateGuide(guide)
    }

    override suspend fun deleteGuide(guideId: String): Result<Unit, AppError> {
        return when (val result = guideFirestoreDataSource.deleteGuide(guideId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    override suspend fun markGuideProgressDirty(guide: Guide, uid: String): Boolean {
        val progress = GuideProgressState.fromGuide(
            guide = guide,
            uid = uid,
            pendingSync = true,
            localUpdatedAt = Date(),
        ) ?: return false
        guideProgressLocalDataSource.upsertGuideProgress(progress)
        return true
    }

    override suspend fun syncPendingGuideProgress(
        familyId: String,
        uid: String,
        force: Boolean,
        guideId: String?,
    ) {
        val pendingItems = guideProgressLocalDataSource.getPendingGuideProgresses(
            familyId = familyId,
            uid = uid,
            guideId = guideId,
        )
        if (pendingItems.isEmpty()) return

        val now = Date()
        pendingItems.forEach { progress ->
            val lastCheckpointTime = progress.lastUploadedAt?.time ?: progress.localUpdatedAt.time
            if (!force && now.time - lastCheckpointTime < MIN_UPLOAD_INTERVAL_MS) {
                return@forEach
            }

            val uploadCandidate = progress.copy(lastUploadedAt = now)
            when (val result = guideFirestoreDataSource.updateGuideProgress(uploadCandidate)) {
                is Result.Success -> {
                    guideProgressLocalDataSource.markGuideProgressSynced(progress.id, now)
                }
                is Result.Failure -> {
                    Log.e(TAG, "Failed syncing guide progress id=${progress.id}: ${result.error}")
                }
            }
        }
    }

    private fun Entity.Guide.toModel() = Guide(
        id = entity.id,
        familyId = entity.familyId,
        itemName = entity.itemName,
        lastUpdated = entity.lastUpdated,
        description = entity.description,
        visibility = entity.visibility,
        ownerUid = entity.ownerUid,
        contentVersion = entity.contentVersion,
        started = entity.started,
        sections = entity.sections,
        resume = entity.resume,
    )
}
