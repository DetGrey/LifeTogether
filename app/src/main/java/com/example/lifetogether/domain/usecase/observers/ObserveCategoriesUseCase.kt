package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.CategoryLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.CategoriesListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveCategoriesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val categoryLocalDataSource: CategoryLocalDataSource,
) {
    fun start(scope: CoroutineScope): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveCategoriesUseCase invoked")
            firestoreDataSource.categoriesSnapshotListener().collect { result ->
                println("categoriesSnapshotListener().collect result: $result")
                when (result) {
                    is CategoriesListener.Success -> {
                        runCatching {
                            categoryLocalDataSource.updateCategories(result.listItems)
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("categoriesSnapshotListener local update failure: ${error.message}")
                        }
                    }
                    is CategoriesListener.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("categoriesSnapshotListener failure: ${result.message}")
                    }
                }
            } 
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
