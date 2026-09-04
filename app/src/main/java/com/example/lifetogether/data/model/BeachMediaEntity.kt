package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.beach.BeachMedia
import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.BEACH_MEDIA_TABLE)
data class BeachMediaEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    @ColumnInfo(name = "beach_album_id")
    val beachAlbumId: String,
    val url: String,
    @ColumnInfo(name = "storage_path")
    val storagePath: String,
    val thumbnail: ByteArray? = null,
    @ColumnInfo(name = "download_state")
    val downloadState: MediaDownloadState = MediaDownloadState.PENDING,
    @ColumnInfo(name = "last_download_attempt")
    val lastDownloadAttempt: Date? = null,
    @ColumnInfo(name = "last_download_error")
    val lastDownloadError: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
) {
    fun toDomain(): BeachMedia = BeachMedia(
        id = id,
        familyId = familyId,
        beachAlbumId = beachAlbumId,
        url = url,
        storagePath = storagePath,
        thumbnail = thumbnail,
        downloadState = downloadState,
        lastDownloadAttempt = lastDownloadAttempt,
        lastDownloadError = lastDownloadError,
        createdAt = createdAt,
        lastUpdated = lastUpdated,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BeachMediaEntity

        if (id != other.id) return false
        if (familyId != other.familyId) return false
        if (beachAlbumId != other.beachAlbumId) return false
        if (url != other.url) return false
        if (storagePath != other.storagePath) return false
        if (downloadState != other.downloadState) return false
        if (lastDownloadAttempt != other.lastDownloadAttempt) return false
        if (lastDownloadError != other.lastDownloadError) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false
        if (createdAt != other.createdAt) return false
        if (lastUpdated != other.lastUpdated) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + familyId.hashCode()
        result = 31 * result + beachAlbumId.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + storagePath.hashCode()
        result = 31 * result + downloadState.hashCode()
        result = 31 * result + (lastDownloadAttempt?.hashCode() ?: 0)
        result = 31 * result + (lastDownloadError?.hashCode() ?: 0)
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        return result
    }
}
