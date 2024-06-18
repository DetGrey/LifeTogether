package com.example.lifetogether.data.local

import com.example.lifetogether.data.model.GroceryListEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    private val groceryListDao: GroceryListDao,
) {

    fun getListItems(uid: String): Flow<List<GroceryListEntity>> {
        return groceryListDao.getItems(uid)
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
