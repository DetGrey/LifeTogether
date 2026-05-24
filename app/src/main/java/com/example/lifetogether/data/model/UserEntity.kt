package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.USER_TABLE)
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val name: String,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
    val birthday: Date? = null,
    @ColumnInfo(name = "family_id")
    val familyId: String? = null,
    @ColumnInfo(name = "image_data")
    val imageData: ByteArray? = null,
    @ColumnInfo(name = "image_url")
    val imageUrl: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserEntity

        if (uid != other.uid) return false
        if (email != other.email) return false
        if (name != other.name) return false
        if (lastUpdated != other.lastUpdated) return false
        if (birthday != other.birthday) return false
        if (familyId != other.familyId) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (imageUrl != other.imageUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uid.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + (birthday?.hashCode() ?: 0)
        result = 31 * result + (familyId?.hashCode() ?: 0)
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        return result
    }
}
