package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.source.GroceryLocalDataSource
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.remote.GroceryFirestoreDataSource
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
    private val groceryFirestoreDataSource: GroceryFirestoreDataSource,
): GroceryRepository {

    override fun observeGroceryItems(familyId: String): Flow<Result<List<GroceryItem>, AppError>> {
        return groceryLocalDataSource.observeGroceryItems(familyId)
            .map { entities ->
                try {
                    val groceryItems = entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                    Result.Success(groceryItems)
                } catch (e: Exception) {
                    Result.Failure(AppErrors.fromThrowable(e))
                }
            }
    }

    override fun syncGroceryItemsFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.grocerySnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> runCatching {
                    if (result.data.isEmpty()) {
                        groceryLocalDataSource.deleteFamilyGroceryItems(familyId)
                    } else {
                        val entities = result.data.map { item ->
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
                        groceryLocalDataSource.updateGroceryList(familyId, entities)
                    }
                    Result.Success(Unit)
                }.getOrElse { error ->
                    Result.Failure(AppErrors.fromThrowable(error))
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override fun syncGrocerySuggestionsFromRemote(): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.grocerySuggestionsSnapshotListener().map { result ->
            when (result) {
                is Result.Success -> runCatching {
                    val entities = result.data.mapNotNull { suggestion ->
                        suggestion.id?.let { id ->
                            GrocerySuggestionEntity(
                                id = id,
                                suggestionName = suggestion.suggestionName,
                                category = suggestion.category,
                                approxPrice = suggestion.approxPrice,
                            )
                        }
                    }
                    groceryLocalDataSource.updateGrocerySuggestions(entities)
                    Result.Success(Unit)
                }.getOrElse { error ->
                    Result.Failure(AppErrors.fromThrowable(error))
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override suspend fun saveItem(item: Item): Result<String, AppError> {
        return groceryFirestoreDataSource.saveGroceryItem(item)
    }

    //todo do not use listName. Maybe use ListQueryType or somthing
    override suspend fun toggleGroceryItemBought(item: GroceryItem): Result<Unit, AppError> {
        return groceryFirestoreDataSource.toggleGroceryItemCompletion(item)
    }

    override suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, AppError> {
        return groceryFirestoreDataSource.deleteGroceryItems(itemIds)
    }

    override fun getGrocerySuggestions(): Flow<Result<List<GrocerySuggestion>, AppError>> {
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
                Result.Failure(AppErrors.fromThrowable(e))
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
