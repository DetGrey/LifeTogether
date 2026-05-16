package com.example.lifetogether.domain.model.mealplanner

import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.lists.MealType
import java.util.Date

data class MealPlan(
    override val id: String,
    override val familyId: String,
    override val itemName: String,
    val date: String,
    val recipeId: String? = null,
    val customMealName: String? = null,
    val mealType: MealType = MealType.DINNER,
    val notes: String = "",
    override var lastUpdated: Date,
    val dateCreated: Date,
) : Item
