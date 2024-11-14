package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.util.Constants

// Assuming you have an Entity for your lists that includes a count
@Entity(tableName = Constants.GROCERY_SUGGESTIONS_TABLE)
data class GrocerySuggestionEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "suggestion_name")
    val suggestionName: String = "",
    @Embedded(prefix = "category_")
    var category: Category? = null,
)
