package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ListItemsResultListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveTipTrackerUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveTipTrackerUseCase invoked")
            firestoreDataSource.tipTrackerSnapshotListener(familyId).collect { result ->
                println("tipTrackerSnapshotListener().collect result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        runCatching {
                            if (result.listItems.isEmpty()) {
                                println("tipTrackerSnapshotListener().collect result: is empty")
                                localDataSource.deleteFamilyTipItems(familyId)
                            } else {
                                localDataSource.updateTipTracker(result.listItems)
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("ObserveTipTrackerUseCase local update failure: ${error.message}")
                        }
                    }
                    is ListItemsResultListener.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("ObserveFirestoreUseCase failure: ${result.message}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
