package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.result.Result

interface AdminRepository {
    suspend fun deleteCategory(category: Category): Result<Unit, String>
    suspend fun addCategory(category: Category): Result<Unit, String>
    suspend fun saveGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, String>
    suspend fun updateGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, String>
    suspend fun deleteGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, String>
}
