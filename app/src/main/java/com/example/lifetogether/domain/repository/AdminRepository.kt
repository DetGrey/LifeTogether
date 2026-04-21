package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.result.Result

interface AdminRepository {
    suspend fun deleteCategory(category: Category): Result<Unit, AppError>
    suspend fun addCategory(category: Category): Result<Unit, AppError>
    suspend fun saveGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError>
    suspend fun updateGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError>
    suspend fun deleteGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError>
}
