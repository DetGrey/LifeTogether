package com.example.lifetogether.data.local

import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
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
    private val familyInformationDao: FamilyInformationDao,
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
            Constants.RECIPES_TABLE -> flow {
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
                Constants.GROCERY_TABLE -> groceryListDao.deleteItems(itemIds)
                else -> {}
            }
            return ResultListener.Success
        } catch (e: Exception) {
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // -------------------------------------------------------------- RECIPES
    suspend fun updateRecipes(
        items: List<Recipe>,
        byteArrays: Map<String, ByteArray>,
    ) {
        println("LocalDataSource updateRecipes(): Trying to add firestore data to Room")

        println("Recipe list: $items")
        var recipeEntityList = items.map { item ->
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

        if (byteArrays.isNotEmpty()) {
            recipeEntityList = recipeEntityList.map { item ->
                item.copy(imageData = if (byteArrays[item.id] != null) byteArrays[item.id] else null)
            }
        }

//        println("recipeEntityList list: ${recipeEntityList.map { listOf(it.itemName, it.tags) }}")

        // Fetch the current items from the Room database
        val currentItems = recipesDao.getItems(items[0].familyId).first()

        for (item in currentItems) {
            if (item.itemName == "Chicken burger") {
                println("chicken burger currentitems: $item")
            }
        }
        for (item in recipeEntityList) {
            if (item.itemName == "Chicken burger") {
                println("chicken burger recipeEntityList: $item")
            }
        }

        // Determine the items to be inserted or updated
        val itemsToUpdate = recipeEntityList.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id && newItem == currentItem }
        }

        println("recipeEntityList itemsToUpdate: ${itemsToUpdate.map { listOf(it.itemName, it.tags) }}")

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

    suspend fun updateUserInformation(
        userInformation: UserInformation,
        byteArray: ByteArray? = null,
    ) {
        var userEntity = UserEntity(
            uid = userInformation.uid ?: "",
            email = userInformation.email,
            name = userInformation.name,
            birthday = userInformation.birthday,
            familyId = userInformation.familyId,
        )
        if (byteArray != null) {
            userEntity = userEntity.copy(imageData = byteArray)
        }

        println("updateUserInformation userEntity: $userEntity")

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

    // -------------------------------------------------------------- FAMILY INFORMATION
    fun getFamilyInformation(familyId: String): Flow<FamilyEntity> {
        return familyInformationDao.getFamilyInfo(familyId)
    }

    fun getFamilyMembers(familyId: String): Flow<List<FamilyMemberEntity>> {
        return familyInformationDao.getFamilyMembers(familyId)
    }

    suspend fun updateFamilyInformation(
        familyInformation: FamilyInformation,
        byteArray: ByteArray? = null,
    ) {
        // Update FamilyEntity
        var familyEntity = FamilyEntity(
            familyId = familyInformation.familyId ?: "",
        )

        if (byteArray != null) {
            familyEntity = familyEntity.copy(imageData = byteArray)
        }

        println("updateFamilyInformation familyEntity: $familyEntity")

        // Update FamilyEntity in the database
        familyInformationDao.updateFamily(familyEntity)

        // Update Family Members
        val familyMembers = familyInformation.members?.map {
            FamilyMemberEntity(
                uid = it.uid ?: "",
                familyId = familyInformation.familyId,
                name = it.name,
            )
        } ?: emptyList()

        // Insert or update FamilyMemberEntity
        familyInformationDao.updateFamilyMembers(familyMembers)
    }

    // -------------------------------------------------------------- IMAGES
    fun getImageByteArray(imageType: ImageType): Flow<ByteArray?> {
        println("LocalDataSource getImageByteArray imageType: $imageType")
        return when (imageType) {
            is ImageType.ProfileImage -> userInformationDao.getImageByteArray(imageType.uid)

            is ImageType.FamilyImage -> familyInformationDao.getImageByteArray(imageType.familyId)

            is ImageType.RecipeImage -> recipesDao.getImageByteArray(imageType.familyId, imageType.recipeId)
        }
    }
}
