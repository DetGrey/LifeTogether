package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants
import java.util.Date

// Assuming you have an Entity for your lists that includes a count
@Entity(tableName = Constants.TIP_TRACKER_TABLE)
data class TipEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "Tip",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    val amount: Float = 0F,
    val currency: String = "DKK",
    val date: Date = Date(),
)
