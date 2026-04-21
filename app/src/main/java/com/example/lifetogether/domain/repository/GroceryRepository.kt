package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface GroceryRepository {
    fun observeGroceryItems(familyId: String): Flow<Result<List<GroceryItem>, AppError>>
    fun syncGroceryItemsFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    fun syncGrocerySuggestionsFromRemote(): Flow<Result<Unit, AppError>>
    suspend fun saveItem(item: Item): Result<String, AppError>
    suspend fun toggleGroceryItemBought(item: GroceryItem): Result<Unit, AppError>
    suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, AppError>
    fun getGrocerySuggestions(): Flow<Result<List<GrocerySuggestion>, AppError>>
//    suspend fun startSync(familyId: String)
}
