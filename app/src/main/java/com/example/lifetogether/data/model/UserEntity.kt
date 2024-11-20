package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.USER_TABLE)
data class UserEntity(
    @PrimaryKey
    val uid: String = "",
    val email: String? = null,
    val name: String? = null,
    val birthday: Date? = null,
    @ColumnInfo(name = "family_id")
    val familyId: String? = null,
    @ColumnInfo(name = "image_data")
    val imageData: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEntity

        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false

        return true
    }

    override fun hashCode(): Int {
        return imageData?.contentHashCode() ?: 0
    }
}
