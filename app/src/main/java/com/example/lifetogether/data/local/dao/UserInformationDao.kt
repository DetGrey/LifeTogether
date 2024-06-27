package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.util.Constants.USER_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInformationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE) // TODO
    suspend fun updateItems(item: UserEntity)

    @Query("SELECT * FROM $USER_TABLE WHERE uid = :uid LIMIT 1")
    fun getItems(uid: String): Flow<UserEntity>

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
