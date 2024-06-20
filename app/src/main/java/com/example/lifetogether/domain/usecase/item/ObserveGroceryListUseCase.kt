package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ListItemsResultListener
import javax.inject.Inject

class ObserveGroceryListUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke() {
        println("ObserveGroceryListUseCase invoked")
        firestoreDataSource.grocerySnapshotListener().collect { result ->
            println("grocerySnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    localDataSource.updateRoomDatabase(result.listItems)
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveGroceryItemsUseCase failure: ${result.message}")
                }
            }
        }
    }
}
