package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.util.Constants.TIP_TRACKER_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface TipTrackerDao {
    @Query("SELECT * FROM $TIP_TRACKER_TABLE")
    fun getAll(): List<TipEntity>

    @Query("SELECT * FROM $TIP_TRACKER_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<TipEntity>>

    @Query("SELECT * FROM $TIP_TRACKER_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemById(familyId: String, id: String): TipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<TipEntity>)

    @Query("DELETE FROM $TIP_TRACKER_TABLE")
    fun deleteTable()

    @Query("DELETE FROM $TIP_TRACKER_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)
}
