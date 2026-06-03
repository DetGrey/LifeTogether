package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.TRAVELLER_PINS_TABLE)
data class TravellerPinEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val type: String,
    @ColumnInfo(name = "date_from")
    val dateFrom: Date?,
    @ColumnInfo(name = "date_to")
    val dateTo: Date?,
    @ColumnInfo(name = "album_id")
    val albumId: String?,
)
