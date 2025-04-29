package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.util.Constants.ALBUMS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumsDao {
    @Query("SELECT * FROM $ALBUMS_TABLE")
    fun getAll(): List<AlbumEntity>

    @Query("SELECT * FROM $ALBUMS_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM $ALBUMS_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getRecipeById(familyId: String, id: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<AlbumEntity>)

    @Query("DELETE FROM $ALBUMS_TABLE")
    fun deleteTable()

    @Query("DELETE FROM $ALBUMS_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)
}
