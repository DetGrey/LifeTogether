package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.Recipe

data class RecipeWithChildren(
    @Embedded
    val recipe: RecipeEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id",
    )
    val ingredients: List<RecipeIngredientEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "recipe_id",
    )
    val instructions: List<RecipeInstructionEntity>,
) {
    fun toDomain(): Recipe {
        return Recipe(
            id = recipe.id,
            familyId = recipe.familyId,
            itemName = recipe.itemName,
            lastUpdated = recipe.lastUpdated,
            description = recipe.description,
            ingredients = ingredients
                .sortedBy { it.sortOrder }
                .map { it.toDomain() },
            instructions = instructions
                .sortedBy { it.sortOrder }
                .map { it.toDomain() },
            preparationTimeMin = recipe.preparationTimeMin,
            favourite = recipe.favourite,
            servings = recipe.servings,
            tags = recipe.tags,
            imageUrl = recipe.imageUrl,
        )
    }
}

@Entity(
    tableName = "recipe_ingredients",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipe_id")],
)
data class RecipeIngredientEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "recipe_id")
    val recipeId: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    val amount: Double,
    @ColumnInfo(name = "measure_type")
    val measureType: String,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    val completed: Boolean,
) {
    fun toDomain(): Ingredient {
        val resolvedMeasureType = MeasureType.entries.firstOrNull { measureTypeEnum ->
            measureTypeEnum.name.equals(measureType, ignoreCase = true) || measureTypeEnum.unit.equals(measureType, ignoreCase = true)
        } ?: MeasureType.PIECE
        return Ingredient(
            id = id,
            amount = amount,
            measureType = resolvedMeasureType,
            itemName = itemName,
            completed = completed,
            sortOrder = sortOrder,
        )
    }
}

@Entity(
    tableName = "recipe_instructions",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipe_id")],
)
data class RecipeInstructionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "recipe_id")
    val recipeId: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    val completed: Boolean,
) {
    fun toDomain(): Instruction {
        return Instruction(
            id = id,
            itemName = itemName,
            completed = completed,
            sortOrder = sortOrder,
        )
    }
}
