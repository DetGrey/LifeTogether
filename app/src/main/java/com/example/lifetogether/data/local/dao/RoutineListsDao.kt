package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.util.Constants.ROUTINE_LIST_ENTRIES_TABLE
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface RoutineListsDao {
    @Query("SELECT * FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<RoutineListEntryEntity>>

    @Query("SELECT * FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE family_id = :familyId AND list_id = :listId")
    fun getItemsByListId(familyId: String, listId: String): Flow<List<RoutineListEntryEntity>>

    @Query("SELECT * FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE id = :id")
    fun getItemById(id: String): Flow<RoutineListEntryEntity?>

    @Query("SELECT * FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE id = :id LIMIT 1")
    suspend fun getItemOnce(id: String): RoutineListEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<RoutineListEntryEntity>)

    @Query("DELETE FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE id IN (:itemIds)")
    suspend fun deleteItems(itemIds: List<String>)

    @Query("DELETE FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE list_id IN (:listIds)")
    suspend fun deleteByListIds(listIds: List<String>)

    @Query("SELECT image_data FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE id = :entryId LIMIT 1")
    fun observeImageByteArray(entryId: String): Flow<ByteArray?>

    @Query("UPDATE $ROUTINE_LIST_ENTRIES_TABLE SET image_data = :imageData, last_updated = :lastUpdated WHERE family_id = :familyId AND id = :entryId")
    suspend fun updateImageByteArray(
        familyId: String,
        entryId: String,
        imageData: ByteArray?,
        lastUpdated: Date,
    )

    @Query("UPDATE $ROUTINE_LIST_ENTRIES_TABLE SET image_url = :imageUrl, last_updated = :lastUpdated WHERE family_id = :familyId AND id = :entryId")
    suspend fun updateImageUrl(
        familyId: String,
        entryId: String,
        imageUrl: String?,
        lastUpdated: Date,
    )

    @Query("DELETE FROM $ROUTINE_LIST_ENTRIES_TABLE WHERE family_id = :familyId")
    suspend fun deleteFamilyItems(familyId: String)
}
