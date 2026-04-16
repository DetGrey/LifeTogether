package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface GroceryRepository {
    suspend fun toggleGroceryItemBought(item: GroceryItem): Result<Unit, String>
    suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, String>
    fun getGrocerySuggestions(): Flow<Result<List<GrocerySuggestion>, String>>
}