package com.example.lifetogether.data.local

// Data Access Object (DAO) for the ListCountEntity
// @Dao
// interface ListCountDao {
//    @Query("SELECT * FROM list_counts")
//    fun getAllListCounts(): LiveData<List<ListCountEntity>>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertListCount(listCount: ListCountEntity)
//
//    @Query("UPDATE list_counts SET itemCount = itemCount + 1 WHERE listName = :listName")
//    suspend fun incrementItemCount(listName: String)
//
//    @Query("UPDATE list_counts SET itemCount = itemCount - 1 WHERE listName = :listName")
//    suspend fun decrementItemCount(listName: String)
// }
