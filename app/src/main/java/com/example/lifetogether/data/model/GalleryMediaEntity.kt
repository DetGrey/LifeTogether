package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.GALLERY_MEDIA_TABLE)
data class GalleryMediaEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "media_type")
    val mediaType: MediaType,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
    @ColumnInfo(name = "album_id")
    val albumId: String,
    @ColumnInfo(name = "date_created")
    val dateCreated: Date,
    @ColumnInfo(name = "media_uri")
    val mediaUri: String? = null,
    @ColumnInfo(name = "remote_media_url")
    val remoteMediaUrl: String? = null,
    @ColumnInfo(name = "download_state")
    val downloadState: MediaDownloadState = MediaDownloadState.PENDING,
    @ColumnInfo(name = "last_download_attempt")
    val lastDownloadAttempt: Date? = null,
    @ColumnInfo(name = "last_download_error")
    val lastDownloadError: String? = null,
    val thumbnail: ByteArray? = null, // Could be used for video thumbnails too if generated locally
    // Video-specific properties (nullable for images)
    @ColumnInfo(name = "video_duration")
    val videoDuration: Long? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryMediaEntity

        if (videoDuration != other.videoDuration) return false
        if (id != other.id) return false
        if (mediaType != other.mediaType) return false
        if (familyId != other.familyId) return false
        if (itemName != other.itemName) return false
        if (lastUpdated != other.lastUpdated) return false
        if (albumId != other.albumId) return false
        if (dateCreated != other.dateCreated) return false
        if (mediaUri != other.mediaUri) return false
        if (remoteMediaUrl != other.remoteMediaUrl) return false
        if (downloadState != other.downloadState) return false
        if (lastDownloadAttempt != other.lastDownloadAttempt) return false
        if (lastDownloadError != other.lastDownloadError) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = videoDuration?.hashCode() ?: 0
        result = 31 * result + id.hashCode()
        result = 31 * result + mediaType.hashCode()
        result = 31 * result + familyId.hashCode()
        result = 31 * result + itemName.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + albumId.hashCode()
        result = 31 * result + (dateCreated.hashCode())
        result = 31 * result + (mediaUri?.hashCode() ?: 0)
        result = 31 * result + (remoteMediaUrl?.hashCode() ?: 0)
        result = 31 * result + downloadState.hashCode()
        result = 31 * result + (lastDownloadAttempt?.hashCode() ?: 0)
        result = 31 * result + (lastDownloadError?.hashCode() ?: 0)
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}
