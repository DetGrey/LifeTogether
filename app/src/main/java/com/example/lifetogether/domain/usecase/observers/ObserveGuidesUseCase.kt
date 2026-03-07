package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.model.guides.Guide
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveGuidesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
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
                    is ListItemsResultListener.Success -> {
                        Log.d(TAG, "shared snapshot success count=${sharedResult.listItems.size}")
                        sharedHasSuccessfulSync = true
                        sharedResult.listItems.also { lastSharedGuides = it }
                    }
                    is ListItemsResultListener.Failure -> {
                        Log.e(TAG, "shared listener failure: ${sharedResult.message}")
                        lastSharedGuides
                    }
                }

                val privateGuides: List<Guide> = when (privateResult) {
                    is ListItemsResultListener.Success -> {
                        Log.d(TAG, "private snapshot success count=${privateResult.listItems.size}")
                        privateHasSuccessfulSync = true
                        privateResult.listItems.also { lastPrivateGuides = it }
                    }
                    is ListItemsResultListener.Failure -> {
                        Log.e(TAG, "private listener failure: ${privateResult.message}")
                        lastPrivateGuides
                    }
                }

                val hadAnySuccessInThisEmission = sharedResult is ListItemsResultListener.Success ||
                    privateResult is ListItemsResultListener.Success
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
                            localDataSource.deleteFamilyGuides(familyId)
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
                            localDataSource.updateGuides(mergedGuides.toList())
                        } else {
                            Log.d(
                                TAG,
                                "merged guides count=${mergedGuides.size} without full coverage -> upsertGuides familyId=$familyId sharedReady=$sharedHasSuccessfulSync privateReady=$privateHasSuccessfulSync",
                            )
                            localDataSource.upsertGuides(mergedGuides.toList())
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
