package com.example.lifetogether.data.local

import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.UserInformation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val groceryListDao: GroceryListDao,
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
    fun getListItems(familyId: String): Flow<List<GroceryListEntity>> {
        val items = groceryListDao.getItems(familyId)
        println("LocalDataSource getListItems: $items")
        return items
    }

    suspend fun updateGroceryList(items: List<GroceryItem>) {
        println("LocalDataSource updateRoomDatabase(): Trying to add firestore data to Room")
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
        groceryListDao.updateItems(groceryListEntityList)

        // Delete items not in the cloud database
        val localItemIds = groceryListEntityList.map { it.id }
        val allLocalItems = groceryListDao.getAllItems() // Fetch all items from the local database
        val itemsToDelete = allLocalItems.filterNot { localItemIds.contains(it.id) }.map { it.id }

        // Delete the items not found in the cloud
        groceryListDao.deleteItems(itemsToDelete)
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
    fun clearUserInformationTable(): ResultListener {
        try {
            groceryListDao.deleteTable()
            userInformationDao.deleteTable()
            return ResultListener.Success
        } catch (e: Exception) {
            return ResultListener.Failure("Error: $e")
        }
    }
}
