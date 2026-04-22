package com.example.lifetogether.domain.usecase.sync

import android.util.Log
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SyncCategoriesUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository,
) {
    private companion object {
        const val TAG = "ObserveCategoriesUC"
    }

    fun start(scope: CoroutineScope): SyncStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            groceryRepository.syncCategories().collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> Log.e(TAG, "categories sync failure: ${result.error}")
                }
            }
        }
        return SyncStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
