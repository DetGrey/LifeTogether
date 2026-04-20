package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.GroceryLocalDataSource
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.collections.map

class GroceryRepositoryImpl @Inject constructor(
    private val groceryLocalDataSource: GroceryLocalDataSource,
    private val firestoreDataSource: FirestoreDataSource,
): GroceryRepository {

    override fun observeGroceryItems(familyId: String): Flow<Result<List<GroceryItem>, String>> {
        return groceryLocalDataSource.observeGroceryItems(familyId)
            .map { entities ->
                try {
                    val groceryItems = entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                    Result.Success(groceryItems)
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override suspend fun saveItem(item: Item): Result<String, String> {
        return firestoreDataSource.saveItem(item, Constants.GROCERY_TABLE)
    }

    //todo do not use listName. Maybe use ListQueryType or somthing
    override suspend fun toggleGroceryItemBought(item: GroceryItem): Result<Unit, String> {
        return firestoreDataSource.toggleCompletableItemCompletion(item, Constants.GROCERY_TABLE)
    }

    override suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, String> {
        return firestoreDataSource.deleteItems(Constants.GROCERY_TABLE, itemIds)
    }

    override fun getGrocerySuggestions(): Flow<Result<List<GrocerySuggestion>, String>> {
        return groceryLocalDataSource.getGrocerySuggestions().map { list ->
            try {
                Result.Success(
                    list.map { grocerySuggestion ->
                        GrocerySuggestion(
                            id = grocerySuggestion.id,
                            suggestionName = grocerySuggestion.suggestionName,
                            category = grocerySuggestion.category,
                            approxPrice = grocerySuggestion.approxPrice,
                        )
                    },
                )
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown error")
            }
        }
    }
    
    private fun GroceryListEntity.toModel() = GroceryItem(
        id = id,
        familyId = familyId,
        itemName = name,
        lastUpdated = lastUpdated,
        completed = completed,
        category = category,
        approxPrice = approxPrice,
    )
}
