package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface GroceryRepository {
    fun observeGroceryItems(familyId: String): Flow<Result<List<GroceryItem>, AppError>>
    fun observeCategories(): Flow<Result<List<Category>, AppError>>
    fun observeGrocerySuggestions(): Flow<Result<List<GrocerySuggestion>, AppError>>
    fun syncGroceryItems(familyId: String): Flow<Result<Unit, AppError>>
    fun syncCategories(): Flow<Result<Unit, AppError>>
    fun syncGrocerySuggestions(): Flow<Result<Unit, AppError>>
    suspend fun saveGroceryItem(item: Item): Result<String, AppError>
    suspend fun toggleGroceryItemBought(item: GroceryItem): Result<Unit, AppError>
    suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, AppError>
    suspend fun addCategory(category: Category): Result<Unit, AppError>
    suspend fun deleteCategory(category: Category): Result<Unit, AppError>
    suspend fun saveGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError>
    suspend fun updateGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError>
    suspend fun deleteGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError>
}
