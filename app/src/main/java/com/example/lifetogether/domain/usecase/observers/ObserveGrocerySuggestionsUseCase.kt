package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.GrocerySuggestionsListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveGrocerySuggestionsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    fun start(scope: CoroutineScope): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveGrocerySuggestionsUseCase invoked")
            firestoreDataSource.grocerySuggestionsSnapshotListener().collect { result ->
                println("grocerySuggestionsSnapshotListener().collect result: $result")
                when (result) {
                    is GrocerySuggestionsListener.Success -> {
                        runCatching {
                            localDataSource.updateGrocerySuggestions(result.listItems)
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("ObserveGrocerySuggestionsUseCase local update failure: ${error.message}")
                        }
                    }
                    is GrocerySuggestionsListener.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("ObserveGrocerySuggestionsUseCase failure: ${result.message}")
                    }
                }
            } 
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
