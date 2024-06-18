package com.example.lifetogether.data.local

// LocalDataSource class
// class LocalDataSource(private val listCountDao: ListCountDao) {
//
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
// }
