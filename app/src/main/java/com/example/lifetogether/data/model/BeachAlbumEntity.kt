package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.beach.BeachAlbum
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.BEACH_ALBUMS_TABLE)
data class BeachAlbumEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    val name: String,
    val count: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
) {
    fun toDomain(): BeachAlbum = BeachAlbum(
        id = id,
        familyId = familyId,
        name = name,
        count = count,
        createdAt = createdAt,
        lastUpdated = lastUpdated,
    )
}
