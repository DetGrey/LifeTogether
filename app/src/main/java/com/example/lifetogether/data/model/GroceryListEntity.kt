package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.util.Constants
import java.util.Date

// Assuming you have an Entity for your lists that includes a count
@Entity(tableName = Constants.GROCERY_TABLE)
data class GroceryListEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    val name: String = "",
    @ColumnInfo(name = "last_updated")
    var lastUpdated: Date = Date(),
    var completed: Boolean = false,
    @Embedded(prefix = "category_")
    var category: Category? = null,
)
