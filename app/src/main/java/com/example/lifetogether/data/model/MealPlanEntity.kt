package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants.MEAL_PLAN_TABLE
import java.util.Date

@Entity(tableName = MEAL_PLAN_TABLE)
data class MealPlanEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    val date: String,
    @ColumnInfo(name = "recipe_id")
    val recipeId: String? = null,
    @ColumnInfo(name = "custom_meal_name")
    val customMealName: String? = null,
    @ColumnInfo(name = "meal_type")
    val mealType: String = "DINNER",
    val notes: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
    @ColumnInfo(name = "date_created")
    val dateCreated: Date,
)
