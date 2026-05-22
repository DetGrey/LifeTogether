package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Item
import java.util.Date

data class Recipe(
    override var id: String,
    override val familyId: String,
    override var itemName: String,
    override val lastUpdated: Date = Date(),
    val description: String,
    val ingredients: List<Ingredient>,
    val instructions: List<Instruction>,
    val preparationTimeMin: Int,
    val favourite: Boolean,
    val servings: Int,
    val tags: List<String>,
    val imageUrl: String? = null,
) : Item
