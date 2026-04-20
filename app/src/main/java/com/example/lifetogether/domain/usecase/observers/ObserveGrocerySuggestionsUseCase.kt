package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.GroceryLocalDataSource
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.remote.FirestoreDataSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveGrocerySuggestionsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val groceryLocalDataSource: GroceryLocalDataSource,
) {
    fun start(scope: CoroutineScope): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveGrocerySuggestionsUseCase invoked")
            firestoreDataSource.grocerySuggestionsSnapshotListener().collect { result ->
                println("grocerySuggestionsSnapshotListener().collect result: $result")
                when (result) {
                    is com.example.lifetogether.domain.result.Result.Success -> {
                        runCatching {
                            val entities = result.data.mapNotNull { suggestion ->
                                suggestion.id?.let { id ->
                                    GrocerySuggestionEntity(
                                        id = id,
                                        suggestionName = suggestion.suggestionName,
                                        category = suggestion.category,
                                        approxPrice = suggestion.approxPrice,
                                    )
                                }
                            }
                            groceryLocalDataSource.updateGrocerySuggestions(entities)
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("ObserveGrocerySuggestionsUseCase local update failure: ${error.message}")
                        }
                    }
                    is com.example.lifetogether.domain.result.Result.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("ObserveGrocerySuggestionsUseCase failure: ${result.error}")
                    }
                }
            } 
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
