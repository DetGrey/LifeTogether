package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveRoutineListsUseCase @Inject constructor(
    private val userListRepository: UserListRepository,
) {
    private companion object {
        const val TAG = "ObserveRoutineListsUseCase"
    }

    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            Log.d(TAG, "invoke familyId=$familyId")
            userListRepository.syncRoutineListEntriesFromRemote(familyId).collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> Log.e(TAG, "routine list sync failure: ${result.error}")
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
