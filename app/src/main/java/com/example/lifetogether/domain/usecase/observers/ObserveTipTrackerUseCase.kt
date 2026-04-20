package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.TipTrackerLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveTipTrackerUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val tipTrackerLocalDataSource: TipTrackerLocalDataSource,
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
                    is AppResult.Success -> {
                        runCatching {
                            if (result.data.items.isEmpty()) {
                                println("tipTrackerSnapshotListener().collect result: is empty")
                                tipTrackerLocalDataSource.deleteFamilyTipItems(familyId)
                            } else {
                                tipTrackerLocalDataSource.updateTipTracker(result.data.items)
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("ObserveTipTrackerUseCase local update failure: ${error.message}")
                        }
                    }
                    is AppResult.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("ObserveFirestoreUseCase failure: ${result.error}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
