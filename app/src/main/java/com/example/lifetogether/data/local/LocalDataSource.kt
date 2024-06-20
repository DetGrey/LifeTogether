package com.example.lifetogether.data.local

import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.domain.model.GroceryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val groceryListDao: GroceryListDao,
) {

    fun getListItems(uid: String): Flow<List<GroceryListEntity>> {
        val items = groceryListDao.getItems(uid)
        println("LocalDataSource getListItems: $items")
        return items
    }

    suspend fun updateRoomDatabase(items: List<GroceryItem>) {
        println("LocalDataSource updateRoomDatabase(): Trying to add firestore data to Room")
        println("GroceryItem list: $items")
        val groceryListEntityList = items.map { item ->
            GroceryListEntity(
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
