package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.GrocerySuggestion
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class LocalListRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
) : ListRepository {

    override suspend fun saveItem(
        item: Item,
        listName: String,
    ): StringResultListener {
        TODO("Not yet implemented")
    }

    fun deleteItems(
        listName: String,
        itemIds: List<String>,
    ): ResultListener {
        println("LocalListRepositoryImpl deleteItems()")
        return localDataSource.deleteItems(listName, itemIds)
    }

    fun getCategories(): Flow<CategoriesListener> {
        println("LocalListRepositoryImpl getCategories()")
        return localDataSource.getCategories().map { list ->
            try {
                CategoriesListener.Success(
                    list.map { category ->
                        Category(
                            emoji = category.emoji,
                            name = category.name,
                        )
                    },
                )
            } catch (e: Exception) {
                CategoriesListener.Failure(e.message ?: "Unknown error")
            }
        }
    }

    fun getGrocerySuggestions(): Flow<GrocerySuggestionsListener> {
        println("LocalListRepositoryImpl getGrocerySuggestions()")
        return localDataSource.getGrocerySuggestions().map { list ->
            println("Grocery suggestions: $list")
            try {
                GrocerySuggestionsListener.Success(
                    list.map { grocerySuggestion ->
                        GrocerySuggestion(
                            id = grocerySuggestion.id,
                            suggestionName = grocerySuggestion.suggestionName,
                            category = grocerySuggestion.category,
                        )
                    },
                )
            } catch (e: Exception) {
                GrocerySuggestionsListener.Failure(e.message ?: "Unknown error")
            }
        }
    }

    fun <T : Item> fetchListItems(
        listName: String,
        familyId: String,
        itemType: KClass<T>,
    ): Flow<ListItemsResultListener<Item>> {
        println("LocalListRepoImpl fetchListItems init")
        return localDataSource.getListItems(listName, familyId)
            .map { entities ->
                try {
                    println("LocalListRepoImpl fetchListItems entities: $entities")
                    // Convert entities to items
                    val itemsList = entities.map { it.toItem(itemType) }
                    println("LocalListRepoImpl after getting items from local data source")
                    for (item in itemsList) {
                        if (item.itemName == "Chicken burger") {
                            println("chicken burger fetched: $item")
                        }
                    }
                    println("fetchListItems of specified itemType: $itemsList")
                    ListItemsResultListener.Success(itemsList)
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                    ListItemsResultListener.Failure(e.message ?: "Unknown error")
                }
            }
    }

    fun fetchItemById(
        listName: String,
        familyId: String,
        id: String,
        itemType: KClass<out Item>,
    ): Flow<ItemResultListener<Item>> {
        return localDataSource.getItemById(listName, familyId, id)
            .map { entity ->
                try {
                    println("LocalListRepoImpl fetchItemById entity: $entity")
                    val item = entity.toItem(itemType)
                    println("fetchItemById of specified itemType: $item")
                    ItemResultListener.Success(item)
                } catch (e: Exception) {
                    ItemResultListener.Failure(e.message ?: "Unknown error")
                }
            }
    }

    // Assuming GroceryItem is a subclass of Item and has a matching constructor
    // TODO ADD MORE ITEM CLASSES
    private fun Entity.toItem(itemType: KClass<out Item>): Item {
        return when (this) {
            is Entity.GroceryList -> when (itemType) {
                GroceryItem::class -> GroceryItem(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.name,
                    lastUpdated = this.entity.lastUpdated,
                    completed = this.entity.completed,
                    category = this.entity.category,
                )
                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }
            is Entity.Recipe -> when (itemType) {
                Recipe::class -> Recipe(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.itemName,
                    lastUpdated = this.entity.lastUpdated,
                    description = this.entity.description,
                    ingredients = this.entity.ingredients,
                    instructions = this.entity.instructions,
                    preparationTimeMin = this.entity.preparationTimeMin,
                    favourite = this.entity.favourite,
                    servings = this.entity.servings,
                    tags = this.entity.tags,
                )
                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }
        }
    }
}
