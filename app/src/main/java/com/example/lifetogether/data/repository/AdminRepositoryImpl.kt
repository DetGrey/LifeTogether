package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.remote.GroceryFirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.repository.AdminRepository
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val groceryFirestoreDataSource: GroceryFirestoreDataSource,
) : AdminRepository {
    override suspend fun deleteCategory(
        category: Category,
    ): Result<Unit, AppError> {
        return groceryFirestoreDataSource.deleteCategory(category)
    }
    override suspend fun addCategory(
        category: Category,
    ): Result<Unit, AppError> {
        return groceryFirestoreDataSource.addCategory(category)
    }
    override suspend fun saveGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): Result<Unit, AppError> {
        return when (val result = groceryFirestoreDataSource.addGrocerySuggestion(grocerySuggestion)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
    override suspend fun updateGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): Result<Unit, AppError> {
        return when (val result = groceryFirestoreDataSource.updateGrocerySuggestion(grocerySuggestion)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
    override suspend fun deleteGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): Result<Unit, AppError> {
        return when (val result = groceryFirestoreDataSource.deleteGrocerySuggestion(grocerySuggestion)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }
}
