package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Item
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Recipe(
    @DocumentId @Transient
    override val id: String? = null,
    override val familyId: String = "",
    override val itemName: String = "",
    override var lastUpdated: Date = Date(),
    val description: String = "",
    val ingredients: List<Ingredient> = listOf(),
    val instructions: List<Instruction> = listOf(),
    val preparationTimeMin: Int = 0,
    val favourite: Boolean = false,
    val servings: Int = 1,
    val tags: List<String> = listOf(),
) : Item

fun Recipe.toMutableRecipe(): MutableRecipe {
    return MutableRecipe().apply {
        itemName = this@toMutableRecipe.itemName
        description = this@toMutableRecipe.description
        ingredients = this@toMutableRecipe.ingredients
        instructions = this@toMutableRecipe.instructions
        preparationTimeMin = this@toMutableRecipe.preparationTimeMin
        favourite = this@toMutableRecipe.favourite
        servings = this@toMutableRecipe.servings
        tags = this@toMutableRecipe.tags
    }
}
