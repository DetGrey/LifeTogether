package com.example.lifetogether.domain.model.lists

import java.util.Date

data class MealPlanEntry(
    override val id: String,
    override val familyId: String,
    override val listId: String,
    override var itemName: String,
    val date: String,
    val recipeId: String? = null,
    val customMealName: String? = null,
    override var lastUpdated: Date,
    override val dateCreated: Date,
) : ListEntry
