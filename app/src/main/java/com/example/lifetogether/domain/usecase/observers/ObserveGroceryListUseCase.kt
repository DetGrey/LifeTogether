package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ListItemsResultListener
import javax.inject.Inject

class ObserveGroceryListUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
    ) {
        println("ObserveGroceryListUseCase invoked")
        firestoreDataSource.grocerySnapshotListener(familyId).collect { result ->
            println("grocerySnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    localDataSource.updateGroceryList(result.listItems)
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveFirestoreUseCase failure: ${result.message}")
                }
            }
        }
    }
}
