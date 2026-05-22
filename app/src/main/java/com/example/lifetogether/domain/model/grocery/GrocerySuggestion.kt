package com.example.lifetogether.domain.model.grocery

import com.example.lifetogether.domain.model.Category
import java.util.Date

data class GrocerySuggestion(
    val id: String,
    val suggestionName: String,
    var category: Category,
    val approxPrice: Float? = null,
    val lastUpdated: Date = Date(),
)
