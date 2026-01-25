package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.data.model.GalleryMediaIdWithUri
import com.example.lifetogether.util.Constants.GALLERY_MEDIA_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryMediaDao {
    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE")
    fun getAll(): List<GalleryMediaEntity>

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<GalleryMediaEntity>>

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId AND id in (:ids)")
    fun getItemsByIds(familyId: String, ids: List<String>): List<GalleryMediaEntity>?

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId AND album_id = :albumId")
    fun getItemsByAlbumId(familyId: String, albumId: String): Flow<List<GalleryMediaEntity>>

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemById(familyId: String, id: String): GalleryMediaEntity?

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirect(id: String): GalleryMediaEntity?

    @Query("SELECT id, media_uri FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId")
    suspend fun getExistingMediaIdsWithUris(familyId: String): List<GalleryMediaIdWithUri>

    @Query("SELECT COUNT(*) FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId AND album_id = :albumId")
    fun getItemCountByAlbumId(familyId: String, albumId: String): Int

    @Query("SELECT id FROM $GALLERY_MEDIA_TABLE WHERE album_id = :albumId ORDER BY date_created DESC LIMIT 1")
    suspend fun getLatestMediaIdForAlbum(albumId: String): String?

    @Query("SELECT thumbnail FROM $GALLERY_MEDIA_TABLE WHERE id = :id LIMIT 1")
    suspend fun getMediaThumbnail(id: String): ByteArray?

    @Query("SELECT thumbnail FROM $GALLERY_MEDIA_TABLE WHERE album_id = :albumId ORDER BY date_created DESC LIMIT 1")
    suspend fun getNewestMediaThumbnailByAlbumId(albumId: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<GalleryMediaEntity>)

    @Query("DELETE FROM $GALLERY_MEDIA_TABLE")
    fun deleteTable()

    @Query("DELETE FROM $GALLERY_MEDIA_TABLE WHERE id IN (:itemIds)")
    fun deleteItems(itemIds: List<String>)
}
