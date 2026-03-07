package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.GuideEntity
import com.example.lifetogether.util.Constants.GUIDES_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface GuidesDao {
    @Query("SELECT * FROM $GUIDES_TABLE")
    fun getAll(): List<GuideEntity>

    @Query("SELECT * FROM $GUIDES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<GuideEntity>>

    @Query("SELECT * FROM $GUIDES_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemById(familyId: String, id: String): GuideEntity?

    @Query("SELECT * FROM $GUIDES_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemByIdFlow(familyId: String, id: String): Flow<GuideEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<GuideEntity>)

    @Query("DELETE FROM $GUIDES_TABLE")
    fun deleteTable()

    @Query("DELETE FROM $GUIDES_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)
}
