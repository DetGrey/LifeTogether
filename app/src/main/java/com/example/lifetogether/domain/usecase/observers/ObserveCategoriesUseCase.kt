package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.domain.repository.CategoryRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    fun start(scope: CoroutineScope): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            categoryRepository.syncCategoriesFromRemote().collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> println("categories sync failure: ${result.error}")
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
