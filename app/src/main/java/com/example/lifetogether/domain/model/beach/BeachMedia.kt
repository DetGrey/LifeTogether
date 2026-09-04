package com.example.lifetogether.domain.model.beach

import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import java.util.Date

data class BeachMedia(
    val id: String,
    val familyId: String,
    val beachAlbumId: String,
    val url: String,
    val storagePath: String,
    val thumbnail: ByteArray? = null,
    val downloadState: MediaDownloadState = MediaDownloadState.PENDING,
    val lastDownloadAttempt: Date? = null,
    val lastDownloadError: String? = null,
    val createdAt: Date = Date(),
    val lastUpdated: Date = Date(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BeachMedia

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
