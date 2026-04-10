package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ListItemsResultListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveRoutineListsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    private companion object {
        const val TAG = "ObserveRoutineListsUseCase"
    }

    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            Log.d(TAG, "invoke familyId=$familyId")
            firestoreDataSource.familyRoutineListEntriesSnapshotListener(familyId).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success -> {
                        Log.d(TAG, "snapshot count=${result.listItems.size}")
                        runCatching {
                            if (result.listItems.isEmpty()) {
                                localDataSource.deleteFamilyRoutineListEntries(familyId)
                            } else {
                                localDataSource.updateRoutineListEntries(result.listItems)
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { Log.e(TAG, "local update failure: ${it.message}", it) }
                    }
                    is ListItemsResultListener.Failure -> {
                        Log.e(TAG, "listener failure: ${result.message}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
