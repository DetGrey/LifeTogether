package com.example.lifetogether.data.model

import androidx.room.ColumnInfo

data class AlbumThumbnailProjection(
    @ColumnInfo(name = "album_id") val albumId: String,
    @ColumnInfo(name = "thumbnail") val thumbnail: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlbumThumbnailProjection

        if (albumId != other.albumId) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = albumId.hashCode()
        result = 31 * result + thumbnail.contentHashCode()
        return result
    }
}
