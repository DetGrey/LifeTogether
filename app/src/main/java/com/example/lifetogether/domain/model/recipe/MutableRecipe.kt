package com.example.lifetogether.domain.model.recipe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Date

class MutableRecipe {
    var id: String by mutableStateOf("")
    var itemName: String by mutableStateOf("")
    var description: String by mutableStateOf("")
    var ingredients: List<Ingredient> by mutableStateOf(listOf())
    var instructions: List<Instruction> by mutableStateOf(listOf())
    var preparationTimeMin: Int by mutableIntStateOf(0)
    var favourite: Boolean by mutableStateOf(false)
    var servings: Int by mutableIntStateOf(1)
    var tags: List<String> by mutableStateOf(listOf())
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
