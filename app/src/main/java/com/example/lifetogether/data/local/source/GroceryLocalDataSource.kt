package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.domain.result.Result
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

    fun observeGroceryItems(familyId: String): Flow<List<GroceryListEntity>> {
        return groceryListDao.getItems(familyId)
    }

    fun getGrocerySuggestions(): Flow<List<GrocerySuggestionEntity>> = grocerySuggestionsDao.getItems()

    suspend fun updateGrocerySuggestions(entities: List<GrocerySuggestionEntity>) {
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

    suspend fun updateGroceryList(
        familyId: String,
        entities: List<GroceryListEntity>
    ) {
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

    fun deleteItems(itemIds: List<String>): Result<Unit, String> =
        try {
            groceryListDao.deleteItems(itemIds)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
}
