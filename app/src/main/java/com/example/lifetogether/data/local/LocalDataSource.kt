package com.example.lifetogether.data.local

import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.UserInformation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val groceryListDao: GroceryListDao,
    private val categoriesDao: CategoriesDao,
    private val userInformationDao: UserInformationDao,
) {
    fun getListItems(uid: String): Flow<List<GroceryListEntity>> {
        val items = groceryListDao.getItems(uid)
        println("LocalDataSource getListItems: $items")
        return items
    }

    suspend fun updateGroceryList(items: List<GroceryItem>) {
        println("LocalDataSource updateRoomDatabase(): Trying to add firestore data to Room")
        println("GroceryItem list: $items")
        val groceryListEntityList = items.map { item ->
            GroceryListEntity(
                id = item.id ?: "",
                uid = item.uid,
                name = item.itemName,
                lastUpdated = item.lastUpdated,
                completed = item.completed,
                category = item.category,
            )
        }
        println("groceryListEntity list: $groceryListEntityList")
        groceryListDao.updateItems(groceryListEntityList)
    }

    suspend fun updateCategories(items: List<Category>) {
        val categoryEntities = items.map { category ->
            CategoryEntity(
                emoji = category.emoji,
                name = category.name,
            )
        }
        categoriesDao.updateItems(categoryEntities)
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

//    // Function to get all list counts
//    fun getAllListCounts(): LiveData<List<ListCountEntity>> {
//        return listCountDao.getAllListCounts()
//    }
//
//    // Function to add a new list count or update an existing one
//    suspend fun updateListCount(listName: String, itemCount: Int) {
//        val listCount = ListCountEntity(listName, itemCount)
//        listCountDao.insertListCount(listCount)
//    }
//
//    // Function to increment the item count for a specific list
//    suspend fun incrementItemCount(listName: String) {
//        listCountDao.incrementItemCount(listName)
//    }
//
//    // Function to decrement the item count for a specific list
//    suspend fun decrementItemCount(listName: String) {
//        listCountDao.decrementItemCount(listName)
//    }
}
