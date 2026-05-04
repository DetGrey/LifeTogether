package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.MEAL_PLAN_ENTRIES_TABLE)
data class MealPlanEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    @ColumnInfo(name = "list_id")
    val listId: String,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    val date: String,
    @ColumnInfo(name = "recipe_id")
    val recipeId: String? = null,
    @ColumnInfo(name = "custom_meal_name")
    val customMealName: String? = null,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
    @ColumnInfo(name = "date_created")
    val dateCreated: Date,
)
