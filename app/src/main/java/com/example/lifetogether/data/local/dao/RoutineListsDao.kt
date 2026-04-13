package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.util.Constants.ROUTINE_LIST_ENTRIES_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineListsDao {
    @Query("SELECT * FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<RoutineListEntryEntity>>

    @Query("SELECT * FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE family_id = :familyId AND list_id = :listId")
    fun getItemsByListId(familyId: String, listId: String): Flow<List<RoutineListEntryEntity>>

    @Query("SELECT * FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE id = :id")
    fun getItemById(id: String,): RoutineListEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<RoutineListEntryEntity>)

    @Query("DELETE FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)

    @Query("DELETE FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE list_id IN (:listIds)")
    fun deleteByListIds(listIds: List<String>)

    @Query("DELETE FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE family_id = :familyId")
    fun deleteFamilyItems(familyId: String)
}
