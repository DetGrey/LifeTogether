package com.example.lifetogether.domain.model.grocery

import com.example.lifetogether.domain.model.Category

data class GrocerySuggestion(
    val id: String,
    val suggestionName: String,
    var category: Category,
    val approxPrice: Float? = null,
)
