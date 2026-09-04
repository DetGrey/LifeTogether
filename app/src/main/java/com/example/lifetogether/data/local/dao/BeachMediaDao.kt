package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.BeachMediaEntity
import com.example.lifetogether.util.Constants.BEACH_MEDIA_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface BeachMediaDao {
    @Query("SELECT * FROM $BEACH_MEDIA_TABLE WHERE family_id = :familyId AND beach_album_id = :albumId ORDER BY created_at DESC")
    fun getItemsByAlbumId(familyId: String, albumId: String): Flow<List<BeachMediaEntity>>

    @Query("SELECT * FROM $BEACH_MEDIA_TABLE WHERE family_id = :familyId AND beach_album_id = :albumId")
    suspend fun getItemsOnceByAlbumId(familyId: String, albumId: String): List<BeachMediaEntity>

    @Query("SELECT * FROM $BEACH_MEDIA_TABLE WHERE family_id = :familyId")
    suspend fun getItemsOnceByFamilyId(familyId: String): List<BeachMediaEntity>

    @Query("SELECT * FROM $BEACH_MEDIA_TABLE WHERE family_id = :familyId AND thumbnail IS NULL")
    suspend fun getItemsWithMissingThumbnails(familyId: String): List<BeachMediaEntity>

    @Query("SELECT * FROM $BEACH_MEDIA_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    suspend fun getItemOnce(familyId: String, id: String): BeachMediaEntity?

    @Query("UPDATE $BEACH_MEDIA_TABLE SET download_state = 'PENDING' WHERE family_id = :familyId AND id IN (:ids)")
    suspend fun markMediaDownloadsPending(familyId: String, ids: List<String>)

    @Query("UPDATE $BEACH_MEDIA_TABLE SET download_state = 'FAILED', last_download_error = :error, last_download_attempt = :attemptDate WHERE id = :id")
    suspend fun markMediaDownloadFailure(id: String, error: String, attemptDate: java.util.Date)

    @Query("UPDATE $BEACH_MEDIA_TABLE SET download_state = 'READY', thumbnail = :thumbnail WHERE id = :id")
    suspend fun markMediaDownloadSuccess(id: String, thumbnail: ByteArray)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<BeachMediaEntity>)

    @Query("DELETE FROM $BEACH_MEDIA_TABLE WHERE family_id = :familyId AND id IN (:itemIds)")
    suspend fun deleteItems(familyId: String, itemIds: List<String>)

    @Query("DELETE FROM $BEACH_MEDIA_TABLE WHERE family_id = :familyId AND beach_album_id = :albumId")
    suspend fun deleteItemsByAlbumId(familyId: String, albumId: String)

    @Query(
        """
        SELECT beach_album_id AS album_id, thumbnail
        FROM (
            SELECT
                beach_album_id,
                thumbnail,
                ROW_NUMBER() OVER (
                    PARTITION BY beach_album_id
                    ORDER BY created_at DESC, id DESC
                ) AS row_num
            FROM $BEACH_MEDIA_TABLE
            WHERE family_id = :familyId
              AND thumbnail IS NOT NULL
        )
        WHERE row_num = 1
        """
    )
    fun observeAlbumThumbnails(familyId: String): Flow<List<com.example.lifetogether.data.model.AlbumThumbnailProjection>>
}
