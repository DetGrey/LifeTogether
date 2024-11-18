package com.example.lifetogether.data.local

import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.GrocerySuggestion
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val groceryListDao: GroceryListDao,
    private val recipesDao: RecipesDao,
    private val grocerySuggestionsDao: GrocerySuggestionsDao,
    private val categoriesDao: CategoriesDao,
    private val userInformationDao: UserInformationDao,
) {
    // -------------------------------------------------------------- CATEGORIES
    fun getCategories(): Flow<List<CategoryEntity>> {
        return categoriesDao.getItems()
    }

    suspend fun updateCategories(items: List<Category>) {
        val categoryEntities = items.map { category ->
            CategoryEntity(
                emoji = category.emoji,
                name = category.name,
            )
        }
        categoriesDao.updateItems(categoryEntities)

        // Fetch the current items from the Room database
        //   getItems() returns a Flow, so you need to use first() to get the current value
        val currentItems = categoriesDao.getItems().first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = categoryEntities.filter { newItem ->
            currentItems.none { currentItem -> newItem.name == currentItem.name && newItem.emoji == currentItem.emoji }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            categoryEntities.none { newItem -> newItem.name == currentItem.name }
        }

        // Update the Room database with the new or changed items
        categoriesDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        categoriesDao.deleteItems(itemsToDelete)
    }

    // -------------------------------------------------------------- ITEMS
    fun getListItems(
        listName: String,
        familyId: String,
    ): Flow<List<Entity>> {
//        val items = groceryListDao.getItems(familyId)
//        println("LocalDataSource getListItems: $items")
//        return items

        println("LocalDataSource getListItems listname: $listName")
        val items: Flow<List<Entity>> = when (listName) {
            Constants.GROCERY_TABLE -> groceryListDao.getItems(familyId).map { list ->
                list.map { Entity.GroceryList(it) }
            }
            Constants.RECIPES_TABLE -> recipesDao.getItems(familyId).map { list ->
                list.map { Entity.Recipe(it) }
            }
            else -> flowOf(emptyList<Entity>()) // Handle the case where the listName doesn't match any known entity
        }
        println("LocalDataSource getListItems: $items")
        return items
    }

    fun getItemById(
        listName: String,
        familyId: String,
        id: String,
    ): Flow<Entity> {
        return when (listName) {
            "recipes" -> flow {
                val recipe = recipesDao.getRecipeById(familyId, id)
                if (recipe != null) {
                    emit(Entity.Recipe(recipe))
                }
            }
            else -> flowOf() // Handle the case where the listName doesn't match any known entity
        }
    }

    fun deleteItems(
        listName: String,
        itemIds: List<String>,
    ): ResultListener {
        println("LocalDataSource deleteItems()")
        try {
            when (listName) {
                "grocery-list" -> groceryListDao.deleteItems(itemIds)
                else -> {}
            }
            return ResultListener.Success
        } catch (e: Exception) {
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // -------------------------------------------------------------- GROCERY LIST
    suspend fun updateRecipes(items: List<Recipe>) {
        println("LocalDataSource updateRecipes(): Trying to add firestore data to Room")

        println("Recipe list: $items")
        val recipeEntityList = items.map { item ->
            RecipeEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                description = item.description,
                ingredients = item.ingredients,
                instructions = item.instructions,
                preparationTimeMin = item.preparationTimeMin,
                favourite = item.favourite,
                servings = item.servings,
                tags = item.tags,
            )
        }
        println("recipeEntityList list: $recipeEntityList")

        // Fetch the current items from the Room database
        val currentItems = recipesDao.getItems(items[0].familyId).first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = recipeEntityList.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id && newItem == currentItem }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            recipeEntityList.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        recipesDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        recipesDao.deleteItems(itemsToDelete.map { it.id })
    }

    // -------------------------------------------------------------- GROCERY LIST
    suspend fun updateGroceryList(items: List<GroceryItem>) {
        println("LocalDataSource updateGroceryList(): Trying to add firestore data to Room")

        println("GroceryItem list: $items")
        val groceryListEntityList = items.map { item ->
            GroceryListEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                name = item.itemName,
                lastUpdated = item.lastUpdated,
                completed = item.completed,
                category = item.category,
            )
        }
        println("groceryListEntity list: $groceryListEntityList")

        // Fetch the current items from the Room database
        val currentItems = groceryListDao.getItems(items[0].familyId).first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = groceryListEntityList.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id && newItem == currentItem }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            groceryListEntityList.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        groceryListDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        groceryListDao.deleteItems(itemsToDelete.map { it.id })
    }

    // -------------------------------------------------------------- GROCERY SUGGESTIONS
    fun getGrocerySuggestions(): Flow<List<GrocerySuggestionEntity>> {
        return grocerySuggestionsDao.getItems()
    }

    suspend fun updateGrocerySuggestions(items: List<GrocerySuggestion>) {
        println("LocalDataSource updateGrocerySuggestions(): Trying to add firestore data to Room")
        val grocerySuggestionEntities = items.mapNotNull { grocerySuggestion ->
            grocerySuggestion.id?.let { id ->
                GrocerySuggestionEntity(
                    id = id,
                    suggestionName = grocerySuggestion.suggestionName,
                    category = grocerySuggestion.category,
                )
            }
        }
        grocerySuggestionsDao.updateItems(grocerySuggestionEntities)

        // Fetch the current items from the Room database
        //   getItems() returns a Flow, so you need to use first() to get the current value
        val currentItems = grocerySuggestionsDao.getItems().first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = grocerySuggestionEntities.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            grocerySuggestionEntities.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        grocerySuggestionsDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        grocerySuggestionsDao.deleteItems(itemsToDelete)
    }

    // -------------------------------------------------------------- USER INFORMATION
    fun getUserInformation(uid: String): Flow<UserEntity> {
        return userInformationDao.getItems(uid)
    }

    suspend fun updateUserInformation(userInformation: UserInformation) {
        val userEntity = UserEntity(
            uid = userInformation.uid ?: "",
            email = userInformation.email,
            name = userInformation.name,
            birthday = userInformation.birthday,
            familyId = userInformation.familyId,
        )

        userInformationDao.updateItems(userEntity)
    }

    fun clearUserInformationTables(): ResultListener {
        try {
            groceryListDao.deleteTable()
            recipesDao.deleteTable()
            userInformationDao.deleteTable()
            return ResultListener.Success
        } catch (e: Exception) {
            return ResultListener.Failure("Error: $e")
        }
    }
}
