package com.example.lifetogether.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.ListCountEntity

// Data Access Object (DAO) for the ListCountEntity
@Dao
interface ListCountDao {
    @Query("SELECT * FROM list_counts")
    fun getAllListCounts(): LiveData<List<ListCountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListCount(listCount: ListCountEntity)

    @Query("UPDATE list_counts SET itemCount = itemCount + 1 WHERE listName = :listName")
    suspend fun incrementItemCount(listName: String)

    @Query("UPDATE list_counts SET itemCount = itemCount - 1 WHERE listName = :listName")
    suspend fun decrementItemCount(listName: String)

//    @Query("SELECT * from items WHERE id = :id")
//    fun getItem(id: Int): Flow<Item>
//
//    @Query("SELECT * from items ORDER BY name ASC")
//    fun getAllItems(): Flow<List<Item>>
}
