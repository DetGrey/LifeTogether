package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.util.Constants.USER_LISTS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface UserListsDao {
    @Query("SELECT * FROM $USER_LISTS_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<UserListEntity>>

    @Query("SELECT * FROM $USER_LISTS_TABLE WHERE id = :id LIMIT 1")
    suspend fun getItemOnce(id: String): UserListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<UserListEntity>)

    @Query("DELETE FROM $USER_LISTS_TABLE WHERE id IN (:itemIds)")
    suspend fun deleteItems(itemIds: List<String>)

    @Query("DELETE FROM $USER_LISTS_TABLE WHERE family_id = :familyId")
    suspend fun deleteFamilyItems(familyId: String)
}
