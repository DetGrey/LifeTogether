package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroceryLocalDataSource @Inject constructor(
    private val groceryListDao: GroceryListDao,
    private val grocerySuggestionsDao: GrocerySuggestionsDao,
) {
    fun getGrocerySuggestions(): Flow<List<GrocerySuggestionEntity>> = grocerySuggestionsDao.getItems()

    suspend fun updateGrocerySuggestions(items: List<GrocerySuggestion>) {
        val entities = items.mapNotNull { suggestion ->
            suggestion.id?.let { id ->
                GrocerySuggestionEntity(
                    id = id,
                    suggestionName = suggestion.suggestionName,
                    category = suggestion.category,
                    approxPrice = suggestion.approxPrice,
                )
            }
        }

        val currentItems = grocerySuggestionsDao.getItems().first()
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        val itemsToDelete = computeItemsToDelete(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )

        grocerySuggestionsDao.updateItems(itemsToUpdate)
        grocerySuggestionsDao.deleteItems(itemsToDelete)
    }

    suspend fun updateGroceryList(items: List<GroceryItem>) {
        val familyId = items.firstOrNull()?.familyId ?: return
        val entities = items.map { item ->
            GroceryListEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                name = item.itemName,
                lastUpdated = item.lastUpdated,
                completed = item.completed,
                category = item.category,
                approxPrice = item.approxPrice,
            )
        }
        val currentItems = groceryListDao.getItems(familyId).first()
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        val itemsToDelete = computeItemsToDelete(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        groceryListDao.updateItems(itemsToUpdate)
        groceryListDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyGroceryItems(familyId: String) {
        groceryListDao.getItems(familyId).firstOrNull()?.let { currentFamilyItems ->
            groceryListDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    fun deleteItems(itemIds: List<String>): ResultListener =
        try {
            groceryListDao.deleteItems(itemIds)
            ResultListener.Success
        } catch (e: Exception) {
            ResultListener.Failure("Error: ${e.message}")
        }
}
