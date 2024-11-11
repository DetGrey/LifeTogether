package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.util.Constants.GROCERY_TABLE
import kotlinx.coroutines.flow.Flow

// Data Access Object (DAO)
@Dao
interface GroceryListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // TODO
    suspend fun updateItems(items: List<GroceryListEntity>)

    @Query("SELECT * FROM $GROCERY_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<GroceryListEntity>>

    @Query("SELECT * FROM $GROCERY_TABLE")
    fun getAllItems(): List<GroceryListEntity>

    @Query("DELETE FROM $GROCERY_TABLE")
    fun deleteTable()

    @Query("DELETE FROM $GROCERY_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)

//    @Query("SELECT * FROM grocery_list")
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
//
//    @Query("SELECT * from items ORDER BY name ASC")
//    fun getAllItems(): Flow<List<Item>>
}
