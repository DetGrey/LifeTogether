package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants

@Entity(tableName = Constants.FAMILIES_TABLE)
data class FamilyEntity(
    @PrimaryKey
    @ColumnInfo(name = "family_id")
    val familyId: String = "",

    @ColumnInfo(name = "image_data")
    val imageData: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FamilyEntity

        if (familyId != other.familyId) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = familyId.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        return result
    }
}
