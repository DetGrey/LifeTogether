package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.NoteEntryEntity
import com.example.lifetogether.util.Constants.NOTE_LIST_ENTRIES_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteEntriesDao {
    @Query("SELECT * FROM $NOTE_LIST_ENTRIES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<NoteEntryEntity>>

    @Query("SELECT * FROM $NOTE_LIST_ENTRIES_TABLE WHERE family_id = :familyId AND list_id = :listId")
    fun getItemsByListId(familyId: String, listId: String): Flow<List<NoteEntryEntity>>

    @Query("SELECT * FROM $NOTE_LIST_ENTRIES_TABLE WHERE id = :id")
    fun getItemById(id: String): Flow<NoteEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<NoteEntryEntity>)

    @Query("DELETE FROM $NOTE_LIST_ENTRIES_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)

    @Query("DELETE FROM $NOTE_LIST_ENTRIES_TABLE WHERE list_id IN (:listIds)")
    fun deleteByListIds(listIds: List<String>)

    @Query("DELETE FROM $NOTE_LIST_ENTRIES_TABLE WHERE family_id = :familyId")
    fun deleteFamilyItems(familyId: String)
}
