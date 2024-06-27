package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.CategoriesListener
import javax.inject.Inject

class ObserveCategoriesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke() {
        println("ObserveCategoriesUseCase invoked")
        firestoreDataSource.categoriesSnapshotListener().collect { result ->
            println("categoriesSnapshotListener().collect result: $result")
            when (result) {
                is CategoriesListener.Success -> {
                    localDataSource.updateCategories(result.listItems)
                }
                is CategoriesListener.Failure -> {
                    // Handle failure
                    println("categoriesSnapshotListener failure: ${result.message}")
                }
            }
        }
    }
}
