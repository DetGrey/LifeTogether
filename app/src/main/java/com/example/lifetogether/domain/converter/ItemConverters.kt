package com.example.lifetogether.domain.converter

import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.model.recipe.MutableRecipe
import com.example.lifetogether.domain.model.recipe.Recipe
import java.util.Date

fun itemToMap(item: Item): Map<String, Any?>? {
    when (item) {
        is Recipe -> {
            return item.toMap()
        }
    }
    return null
}

// ------------------------------------------------------------------------------- ITEMS
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

fun Ingredient.toMap(): Map<String, Any?> {
    return mapOf(
        "amount" to amount,
        "measureType" to measureType,
        "itemName" to itemName,
        "completed" to completed,
    )
}

fun Instruction.toMap(): Map<String, Any?> {
    return mapOf(
        "itemName" to itemName,
        "completed" to completed,
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

fun MutableRecipe.toRecipe(familyId: String = "", lastUpdated: Date = Date()): Recipe {
    return Recipe(
        id = id,
        familyId = familyId,
        itemName = this.itemName,
        lastUpdated = lastUpdated,
        description = this.description,
        ingredients = this.ingredients,
        instructions = this.instructions,
        preparationTimeMin = this.preparationTimeMin,
        favourite = this.favourite,
        servings = this.servings,
        tags = this.tags,
    )
}
