package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.data.local.source.GuideLocalDataSource
import com.example.lifetogether.data.local.source.GuideProgressLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveGuidesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val guideLocalDataSource: GuideLocalDataSource,
    private val guideProgressLocalDataSource: GuideProgressLocalDataSource,
) {
    private companion object {
        const val TAG = "ObserveGuidesUseCase"
    }

    fun start(
        scope: CoroutineScope,
        uid: String,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            Log.d(TAG, "invoke uid=$uid familyId=$familyId")
            launch {
                firestoreDataSource.guideProgressSnapshotListener(familyId, uid).collect { progressResult ->
                    when (progressResult) {
                    is AppResult.Success -> {
                        runCatching {
                            guideProgressLocalDataSource.updateGuideProgressFromRemote(
                                familyId = familyId,
                                uid = uid,
                                items = progressResult.data,
                                )
                            }.onFailure { error ->
                                Log.e(TAG, "Guide progress local update failure: ${error.message}", error)
                            }
                        }

                    is AppResult.Failure -> {
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
                firestoreDataSource.familySharedGuidesSnapshotListener(familyId),
                firestoreDataSource.privateGuidesSnapshotListener(familyId, uid),
            ) { sharedResult, privateResult ->
                sharedResult to privateResult
            }.collect { (sharedResult, privateResult) ->
                val sharedGuides: List<Guide> = when (sharedResult) {
                    is AppResult.Success -> {
                        Log.d(TAG, "shared snapshot success count=${sharedResult.data.items.size}")
                        sharedHasSuccessfulSync = true
                        sharedResult.data.items.also { lastSharedGuides = it }
                    }
                    is AppResult.Failure -> {
                        Log.e(TAG, "shared listener failure: ${sharedResult.error}")
                        lastSharedGuides
                    }
                }

                val privateGuides: List<Guide> = when (privateResult) {
                    is AppResult.Success -> {
                        Log.d(TAG, "private snapshot success count=${privateResult.data.items.size}")
                        privateHasSuccessfulSync = true
                        privateResult.data.items.also { lastPrivateGuides = it }
                    }
                    is AppResult.Failure -> {
                        Log.e(TAG, "private listener failure: ${privateResult.error}")
                        lastPrivateGuides
                    }
                }

                val hadAnySuccessInThisEmission = sharedResult is AppResult.Success ||
                    privateResult is AppResult.Success
                val hasAnySuccessfulSync = sharedHasSuccessfulSync || privateHasSuccessfulSync
                if (!hadAnySuccessInThisEmission && !hasAnySuccessfulSync && sharedGuides.isEmpty() && privateGuides.isEmpty()) {
                    Log.w(TAG, "both guides listeners failed and no cached fallback exists; skipping local update")
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
                            Log.d(TAG, "merged guides empty with full coverage -> deleteFamilyGuides familyId=$familyId")
                            guideLocalDataSource.deleteFamilyGuides(familyId)
                        } else {
                            Log.d(
                                TAG,
                                "merged guides empty without full coverage -> skipping delete familyId=$familyId sharedReady=$sharedHasSuccessfulSync privateReady=$privateHasSuccessfulSync",
                            )
                        }
                    } else {
                        if (hasFullSnapshotCoverage) {
                            Log.d(
                                TAG,
                                "merged guides count=${mergedGuides.size} with full coverage -> updateGuides familyId=$familyId",
                            )
                            guideLocalDataSource.updateGuides(mergedGuides.toList())
                        } else {
                            Log.d(
                                TAG,
                                "merged guides count=${mergedGuides.size} without full coverage -> upsertGuides familyId=$familyId sharedReady=$sharedHasSuccessfulSync privateReady=$privateHasSuccessfulSync",
                            )
                            guideLocalDataSource.upsertGuides(mergedGuides.toList())
                        }
                    }
                }.onSuccess {
                    if (hasAnySuccessfulSync) {
                        firstSuccess.completeFirstSuccessIfNeeded()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "ObserveGuidesUseCase local update failure: ${error.message}", error)
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
