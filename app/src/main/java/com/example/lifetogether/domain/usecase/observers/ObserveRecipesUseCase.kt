package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ListItemsResultListener
import javax.inject.Inject

class ObserveRecipesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
    ) {
        println("ObserveRecipesUseCase invoked")
        firestoreDataSource.recipeSnapshotListener(familyId).collect { result ->
            println("recipeSnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    if (result.listItems.isEmpty()) {
                        println("recipeSnapshotListener().collect result: is empty")
                    } else {
                        localDataSource.updateRecipes(result.listItems)
                    }
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveRecipesUseCase failure: ${result.message}")
                }
            }
        }
    }
}
