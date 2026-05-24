package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.source.GroceryLocalDataSource
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.remote.GroceryFirestoreDataSource
import com.example.lifetogether.data.repository.internal.stampNow
import com.example.lifetogether.domain.model.Category
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
                is Result.Success -> appResultOfSuspend {
                    if (result.data.isEmpty()) {
                        groceryLocalDataSource.deleteFamilyGroceryItems(familyId)
                    } else {
                        val entities = result.data.map { item ->
                            GroceryListEntity(
                                id = item.id,
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
                is Result.Success -> appResultOfSuspend {
                    groceryLocalDataSource.updateCategories(result.data)
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override fun syncGrocerySuggestions(): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.syncGrocerySuggestions().map { result ->
            when (result) {
                is Result.Success -> appResultOfSuspend {
                    val entities = result.data.map { suggestion ->
                        GrocerySuggestionEntity(
                            id = suggestion.id,
                            suggestionName = suggestion.suggestionName,
                            category = suggestion.category,
                            approxPrice = suggestion.approxPrice,
                            lastUpdated = suggestion.lastUpdated,
                        )
                    }
                    groceryLocalDataSource.updateGrocerySuggestions(entities)
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override suspend fun saveGroceryItem(item: GroceryItem): Result<String, AppError> {
        val stampedItem = item.stampNow()
        groceryLocalDataSource.upsertGroceryItem(stampedItem.toEntity())
        return when (val result = groceryFirestoreDataSource.saveGroceryItem(stampedItem)) {
            is Result.Success -> Result.Success(stampedItem.id)
            is Result.Failure -> {
                groceryLocalDataSource.deleteGroceryItem(stampedItem.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun toggleGroceryItemBought(item: GroceryItem): Result<Unit, AppError> {
        val stampedItem = item.stampNow()
        val oldEntity = groceryLocalDataSource.getGroceryItemOnce(item.id)
        groceryLocalDataSource.upsertGroceryItem(stampedItem.toEntity())
        return when (val result = groceryFirestoreDataSource.toggleGroceryItemCompletion(stampedItem)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) groceryLocalDataSource.upsertGroceryItem(oldEntity)
                else groceryLocalDataSource.deleteGroceryItem(stampedItem.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, AppError> {
        val oldEntities = itemIds.mapNotNull { groceryLocalDataSource.getGroceryItemOnce(it) }
        itemIds.forEach { groceryLocalDataSource.deleteGroceryItem(it) }
        return when (val result = groceryFirestoreDataSource.deleteGroceryItems(itemIds)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                oldEntities.forEach { groceryLocalDataSource.upsertGroceryItem(it) }
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun addCategory(category: Category): Result<Unit, AppError> {
        val stamped = category.stampNow()
        groceryLocalDataSource.upsertCategory(stamped)
        return when (val result = groceryFirestoreDataSource.addCategory(stamped)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                groceryLocalDataSource.deleteCategory(stamped.toEntity())
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteCategory(category: Category): Result<Unit, AppError> {
        val entity = category.toEntity()
        groceryLocalDataSource.deleteCategory(entity)
        return when (val result = groceryFirestoreDataSource.deleteCategory(category)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                groceryLocalDataSource.upsertCategory(category)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun saveGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError> {
        val stamped = grocerySuggestion.stampNow()
        groceryLocalDataSource.upsertSuggestion(stamped.toEntity())
        return when (val result = groceryFirestoreDataSource.saveGrocerySuggestion(stamped)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                groceryLocalDataSource.deleteSuggestion(stamped.toEntity())
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError> {
        val stamped = grocerySuggestion.stampNow()
        val oldEntity = groceryLocalDataSource.getSuggestionOnce(stamped.id)
        groceryLocalDataSource.upsertSuggestion(stamped.toEntity())
        return when (val result = groceryFirestoreDataSource.updateGrocerySuggestion(stamped)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) groceryLocalDataSource.upsertSuggestion(oldEntity)
                else groceryLocalDataSource.deleteSuggestion(stamped.toEntity())
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteGrocerySuggestion(grocerySuggestion: GrocerySuggestion): Result<Unit, AppError> {
        val entity = grocerySuggestion.toEntity()
        groceryLocalDataSource.deleteSuggestion(entity)
        return when (val result = groceryFirestoreDataSource.deleteGrocerySuggestion(grocerySuggestion)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                groceryLocalDataSource.upsertSuggestion(entity)
                Result.Failure(result.error)
            }
        }
    }
    
    private fun GroceryItem.toEntity() = GroceryListEntity(
        id = id,
        familyId = familyId,
        name = itemName,
        lastUpdated = lastUpdated,
        completed = completed,
        category = category,
        approxPrice = approxPrice,
    )

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
        lastUpdated = lastUpdated,
    )

    private fun Category.toEntity() = CategoryEntity(
        emoji = emoji,
        name = name,
        lastUpdated = lastUpdated,
    )

    private fun GrocerySuggestionEntity.toModel() = GrocerySuggestion(
        id = id,
        suggestionName = suggestionName,
        category = category,
        approxPrice = approxPrice,
        lastUpdated = lastUpdated,
    )

    private fun GrocerySuggestion.toEntity() = GrocerySuggestionEntity(
        id = id,
        suggestionName = suggestionName,
        category = category,
        approxPrice = approxPrice,
        lastUpdated = lastUpdated,
    )
}
