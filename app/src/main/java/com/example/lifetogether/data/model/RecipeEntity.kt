package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.util.Constants
import java.util.Date

// Assuming you have an Entity for your lists that includes a count
@Entity(tableName = Constants.RECIPES_TABLE)
data class RecipeEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    val description: String = "",
    val ingredients: List<Ingredient> = listOf(),
    val instructions: List<Instruction> = listOf(),
    @ColumnInfo(name = "preparation_time_min")
    val preparationTimeMin: Int = 0,
    val favourite: Boolean = false,
    val servings: Int = 1,
    val tags: List<String> = listOf(),
)
