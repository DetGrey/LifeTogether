package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.GALLERY_IMAGES_TABLE)
data class GalleryImageEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    @ColumnInfo(name = "album_id")
    val albumId: String = "",
    @ColumnInfo(name = "date_created")
    val dateCreated: Date? = null,
    @ColumnInfo(name = "image_data")
    val imageData: ByteArray? = null,

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryImageEntity

        if (id != other.id) return false
        if (familyId != other.familyId) return false
        if (itemName != other.itemName) return false
        if (lastUpdated != other.lastUpdated) return false
        if (albumId != other.albumId) return false
        if (dateCreated != other.dateCreated) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + familyId.hashCode()
        result = 31 * result + itemName.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + albumId.hashCode()
        result = 31 * result + (dateCreated?.hashCode() ?: 0)
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        return result
    }
}