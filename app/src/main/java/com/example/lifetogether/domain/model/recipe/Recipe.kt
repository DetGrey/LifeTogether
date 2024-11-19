package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Item
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Recipe(
    @DocumentId @Transient
    override var id: String? = null,
    override val familyId: String = "",
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    val description: String = "",
    val ingredients: List<Ingredient> = listOf(),
    val instructions: List<Instruction> = listOf(),
    val preparationTimeMin: Int = 0,
    val favourite: Boolean = false,
    val servings: Int = 1,
    val tags: List<String> = listOf(),
) : Item

fun Recipe.toMap(): Map<String, Any?> {
    return mapOf(
        "familyId" to familyId,
        "itemName" to itemName,
        "lastUpdated" to lastUpdated,
        "description" to description,
        "ingredients" to ingredients.map { it.toMap() },
        "instructions" to instructions.map { it.toMap() },
        "preparationTimeMin" to preparationTimeMin,
        "favourite" to favourite,
        "servings" to servings,
        "tags" to tags,
    )
}

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
