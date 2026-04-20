package com.example.lifetogether.data.repository

import android.util.Log
import com.example.lifetogether.data.local.source.GuideLocalDataSource
import com.example.lifetogether.data.local.source.GuideProgressLocalDataSource
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideProgressState
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

class GuideRepositoryImpl @Inject constructor(
    private val guideLocalDataSource: GuideLocalDataSource,
    private val guideProgressLocalDataSource: GuideProgressLocalDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) : GuideRepository {

    private companion object {
        const val TAG = "GuideRepositoryImpl"
        const val MIN_UPLOAD_INTERVAL_MS = 2 * 60 * 1000L
    }

    override fun observeGuides(familyId: String, uid: String): Flow<Result<List<Guide>, String>> {
        return guideLocalDataSource.getItems(familyId, uid)
            .map { entities ->
                try {
                    val guides = entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                    Result.Success(guides)
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override fun observeGuideById(familyId: String, id: String, uid: String): Flow<Result<Guide, String>> {
        return guideLocalDataSource.getItemById(familyId, id, uid)
            .map { entity ->
                try {
                    Result.Success(entity.toModel())
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override suspend fun saveGuide(guide: Guide): Result<String, String> {
        return firestoreDataSource.saveItem(guide, Constants.GUIDES_TABLE)
    }

    override suspend fun updateGuide(guide: Guide): Result<Unit, String> {
        return firestoreDataSource.updateItem(guide, Constants.GUIDES_TABLE)
    }

    override suspend fun deleteGuide(guideId: String): Result<Unit, String> {
        return when (val result = firestoreDataSource.deleteItem(guideId, Constants.GUIDES_TABLE)) {
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
            when (val result = firestoreDataSource.updateGuideProgress(uploadCandidate)) {
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
