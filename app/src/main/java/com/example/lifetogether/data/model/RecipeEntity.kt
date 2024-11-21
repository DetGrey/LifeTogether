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
    @ColumnInfo(name = "image_data")
    val imageData: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecipeEntity

        if (id != other.id) return false
        if (familyId != other.familyId) return false
        if (itemName != other.itemName) return false
        if (lastUpdated != other.lastUpdated) return false
        if (description != other.description) return false
        if (ingredients != other.ingredients) return false
        if (instructions != other.instructions) return false
        if (preparationTimeMin != other.preparationTimeMin) return false
        if (favourite != other.favourite) return false
        if (servings != other.servings) return false
        if (tags != other.tags) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + familyId.hashCode()
        result = 31 * result + itemName.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + ingredients.hashCode()
        result = 31 * result + instructions.hashCode()
        result = 31 * result + preparationTimeMin
        result = 31 * result + favourite.hashCode()
        result = 31 * result + servings
        result = 31 * result + tags.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        return result
    }
}
