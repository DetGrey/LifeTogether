package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.GalleryImageEntity
import com.example.lifetogether.util.Constants.GALLERY_IMAGES_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryImagesDao {
    @Query("SELECT * FROM $GALLERY_IMAGES_TABLE")
    fun getAll(): List<GalleryImageEntity>

    @Query("SELECT * FROM $GALLERY_IMAGES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<GalleryImageEntity>>

    @Query("SELECT * FROM $GALLERY_IMAGES_TABLE WHERE family_id = :familyId AND album_id = :albumId")
    fun getItemsByAlbumId(familyId: String, albumId: String): Flow<List<GalleryImageEntity>>

    @Query("SELECT * FROM $GALLERY_IMAGES_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemById(familyId: String, id: String): GalleryImageEntity?

    @Query("SELECT COUNT(*) FROM $GALLERY_IMAGES_TABLE WHERE family_id = :familyId AND album_id = :albumId")
    fun getItemCountByAlbumId(familyId: String, albumId: String): Int

    @Query("SELECT thumbnail FROM $GALLERY_IMAGES_TABLE WHERE id = :id LIMIT 1")
    suspend fun getImageThumbnail(id: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<GalleryImageEntity>)

    @Query("DELETE FROM $GALLERY_IMAGES_TABLE")
    fun deleteTable()

    @Query("DELETE FROM $GALLERY_IMAGES_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)
}
