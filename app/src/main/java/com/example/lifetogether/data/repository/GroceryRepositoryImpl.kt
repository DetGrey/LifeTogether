package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.appResultOf

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.source.GroceryLocalDataSource
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.remote.GroceryFirestoreDataSource
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result
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
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                }
            }
    }

    override fun observeCategories(): Flow<Result<List<Category>, AppError>> {
        return groceryLocalDataSource.observeCategories().map { entities ->
            appResultOf {
                entities.map { it.toModel() }
            }
        }
    }

    override fun observeGrocerySuggestions(): Flow<Result<List<GrocerySuggestion>, AppError>> {
        return groceryLocalDataSource.observeGrocerySuggestions().map { entities ->
            appResultOf {
                entities.map { it.toModel() }
            }
        }
    }

    override fun syncGroceryItems(familyId: String): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.syncGroceryItems(familyId).map { result ->
            when (result) {
                is Result.Success -> appResultOf {
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
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override fun syncCategories(): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.syncCategories().map { result ->
            when (result) {
                is Result.Success -> appResultOf {
                    groceryLocalDataSource.updateCategories(result.data)
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override fun syncGrocerySuggestions(): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.syncGrocerySuggestions().map { result ->
            when (result) {
                is Result.Success -> appResultOf {
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
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override suspend fun saveGroceryItem(item: Item): Result<String, AppError> {
        return groceryFirestoreDataSource.saveGroceryItem(item)
    }

    override suspend fun toggleGroceryItemBought(item: GroceryItem): Result<Unit, AppError> {
        return groceryFirestoreDataSource.toggleGroceryItemCompletion(item)
    }

    override suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, AppError> {
        return groceryFirestoreDataSource.deleteGroceryItems(itemIds)
    }

    override suspend fun addCategory(category: Category): Result<Unit, AppError> {
        return groceryFirestoreDataSource.addCategory(category)
    }

    override suspend fun deleteCategory(category: Category): Result<Unit, AppError> {
        return groceryFirestoreDataSource.deleteCategory(category)
    }

    override suspend fun saveGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError> {
        return groceryFirestoreDataSource.saveGrocerySuggestion(grocerySuggestion)
    }

    override suspend fun updateGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError> {
        return groceryFirestoreDataSource.updateGrocerySuggestion(grocerySuggestion)
    }

    override suspend fun deleteGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError> {
        return groceryFirestoreDataSource.deleteGrocerySuggestion(grocerySuggestion)
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

    private fun CategoryEntity.toModel() = Category(
        emoji = emoji,
        name = name,
    )

    private fun GrocerySuggestionEntity.toModel() = GrocerySuggestion(
        id = id,
        suggestionName = suggestionName,
        category = category,
        approxPrice = approxPrice,
    )
}
