package com.example.lifetogether.ui.model

import java.util.Date

data class AlbumUiModel (
    val id: String,
    val familyId: String,
    val name: String,
    val lastUpdated: Date,
    val mediaCount: Int = 0,
    val thumbnail: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlbumUiModel

        if (mediaCount != other.mediaCount) return false
        if (id != other.id) return false
        if (familyId != other.familyId) return false
        if (name != other.name) return false
        if (lastUpdated != other.lastUpdated) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mediaCount
        result = 31 * result + id.hashCode()
        result = 31 * result + familyId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}