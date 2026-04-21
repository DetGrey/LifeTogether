package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveGrocerySuggestionsUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository,
) {
    private companion object {
        const val TAG = "ObserveGrocerySuggUC"
    }

    fun start(scope: CoroutineScope): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            groceryRepository.syncGrocerySuggestionsFromRemote().collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> Log.e(TAG, "grocery suggestions sync failure: ${result.error}")
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
