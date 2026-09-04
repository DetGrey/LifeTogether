package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.BeachAlbumEntity
import com.example.lifetogether.util.Constants.BEACH_ALBUMS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface BeachAlbumsDao {
    @Query("SELECT * FROM $BEACH_ALBUMS_TABLE WHERE family_id = :familyId ORDER BY created_at DESC")
    fun getItems(familyId: String): Flow<List<BeachAlbumEntity>>

    @Query("SELECT * FROM $BEACH_ALBUMS_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemByIdFlow(familyId: String, id: String): Flow<BeachAlbumEntity?>

    @Query("SELECT * FROM $BEACH_ALBUMS_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    suspend fun getItemOnce(familyId: String, id: String): BeachAlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<BeachAlbumEntity>)

    @Query("DELETE FROM $BEACH_ALBUMS_TABLE WHERE family_id = :familyId AND id IN (:itemIds)")
    suspend fun deleteItems(familyId: String, itemIds: List<String>)
}
