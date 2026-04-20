package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.repository.AdminRepository
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
) : AdminRepository {
    override suspend fun deleteCategory(
        category: Category,
    ): Result<Unit, String> {
        return firestoreDataSource.deleteCategory(category)
    }
    override suspend fun addCategory(
        category: Category,
    ): Result<Unit, String> {
        return firestoreDataSource.addCategory(category)
    }
    override suspend fun saveGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): Result<Unit, String> {
        return when (val result = firestoreDataSource.addGrocerySuggestion(grocerySuggestion)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
    override suspend fun updateGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): Result<Unit, String> {
        return when (val result = firestoreDataSource.updateGrocerySuggestion(grocerySuggestion)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
    override suspend fun deleteGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): Result<Unit, String> {
        return when (val result = firestoreDataSource.deleteGrocerySuggestion(grocerySuggestion)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
}
