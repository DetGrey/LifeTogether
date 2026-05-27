package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.AlbumThumbnailProjection
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.util.Constants.GALLERY_MEDIA_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface GalleryMediaDao {
    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE")
    fun getAll(): List<GalleryMediaEntity>

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<GalleryMediaEntity>>

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId AND id in (:ids)")
    fun getItemsByIds(familyId: String, ids: List<String>): List<GalleryMediaEntity>

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId AND album_id = :albumId ORDER BY date_created DESC")
    fun getItemsByAlbumId(familyId: String, albumId: String): Flow<List<GalleryMediaEntity>>

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemById(familyId: String, id: String): GalleryMediaEntity?

    @Query("SELECT * FROM $GALLERY_MEDIA_TABLE WHERE id = :id LIMIT 1")
    suspend fun getItemByIdDirect(id: String): GalleryMediaEntity?

    @Query("SELECT id FROM $GALLERY_MEDIA_TABLE WHERE album_id = :albumId ORDER BY date_created DESC, id DESC LIMIT 1")
    suspend fun getLatestMediaIdForAlbum(albumId: String): String?

    @Query("SELECT thumbnail FROM $GALLERY_MEDIA_TABLE WHERE id = :id LIMIT 1")
    suspend fun getMediaThumbnail(id: String): ByteArray?

    @Query("SELECT thumbnail FROM $GALLERY_MEDIA_TABLE WHERE album_id = :albumId ORDER BY date_created DESC, id DESC LIMIT 1")
    suspend fun getNewestMediaThumbnailByAlbumId(albumId: String): ByteArray?

    @Query(
        """
        SELECT album_id, thumbnail
        FROM (
            SELECT
                album_id,
                thumbnail,
                ROW_NUMBER() OVER (
                    PARTITION BY album_id
                    ORDER BY date_created DESC, id DESC
                ) AS row_num
            FROM $GALLERY_MEDIA_TABLE
            WHERE family_id = :familyId
              AND thumbnail IS NOT NULL
        )
        WHERE row_num = 1
        """
    )
    fun observeAlbumThumbnails(familyId: String): Flow<List<AlbumThumbnailProjection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<GalleryMediaEntity>)

    @Query("DELETE FROM $GALLERY_MEDIA_TABLE WHERE id IN (:itemIds)")
    suspend fun deleteItems(itemIds: List<String>)
}
