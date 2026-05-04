package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.ChecklistEntryEntity
import com.example.lifetogether.util.Constants.CHECKLIST_ENTRIES_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistEntriesDao {
    @Query("SELECT * FROM $CHECKLIST_ENTRIES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<ChecklistEntryEntity>>

    @Query("SELECT * FROM $CHECKLIST_ENTRIES_TABLE WHERE family_id = :familyId AND list_id = :listId")
    fun getItemsByListId(familyId: String, listId: String): Flow<List<ChecklistEntryEntity>>

    @Query("SELECT * FROM $CHECKLIST_ENTRIES_TABLE WHERE id = :id")
    fun getItemById(id: String): Flow<ChecklistEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<ChecklistEntryEntity>)

    @Query("DELETE FROM $CHECKLIST_ENTRIES_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)

    @Query("DELETE FROM $CHECKLIST_ENTRIES_TABLE WHERE list_id IN (:listIds)")
    fun deleteByListIds(listIds: List<String>)

    @Query("DELETE FROM $CHECKLIST_ENTRIES_TABLE WHERE family_id = :familyId")
    fun deleteFamilyItems(familyId: String)
}
