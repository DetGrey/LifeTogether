package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.GROCERY_TABLE)
data class GroceryListEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    val name: String,
    @ColumnInfo(name = "last_updated")
    var lastUpdated: Date,
    var completed: Boolean = false,
    @Embedded(prefix = "category_")
    var category: Category,
    @ColumnInfo(name = "approx_price")
    val approxPrice: Float? = null,
)
