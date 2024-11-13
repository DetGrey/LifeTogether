package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import javax.inject.Inject

class ObserveGrocerySuggestionsUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke() {
        println("ObserveGrocerySuggestionsUseCase invoked")
        firestoreDataSource.grocerySuggestionsSnapshotListener().collect { result ->
            println("grocerySuggestionsSnapshotListener().collect result: $result")
            when (result) {
                is GrocerySuggestionsListener.Success -> {
                    localDataSource.updateGrocerySuggestions(result.listItems)
                }
                is GrocerySuggestionsListener.Failure -> {
                    // Handle failure
                    println("ObserveFirestoreUseCase failure: ${result.message}")
                }
            }
        }
    }
}
