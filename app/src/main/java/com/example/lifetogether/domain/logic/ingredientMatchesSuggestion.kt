package com.example.lifetogether.domain.logic

fun ingredientMatchesSuggestion(ingredientName: String, suggestionName: String): Boolean {
    val a = ingredientName.trim().lowercase()
    val b = suggestionName.trim().lowercase()
    return a == b || a + "s" == b || a + "es" == b || b + "s" == a || b + "es" == a
}
