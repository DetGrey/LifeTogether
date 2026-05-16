package com.example.lifetogether.ui.feature.lists.entryDetails

fun searchMealRecipeSuggestions(
    query: String,
    suggestions: List<RecipeSearchItem>,
    selectedRecipeId: String? = null,
    limit: Int = 5,
): List<RecipeSearchItem> {
    val normalizedQuery = normalize(query)
    if (normalizedQuery.isEmpty()) return emptyList()

    return suggestions
        .asSequence()
        .filterNot { it.id == selectedRecipeId }
        .map { suggestion -> suggestion to scoreMealRecipeSuggestion(normalizedQuery, suggestion) }
        .filter { (_, score) -> score > 0 }
        .sortedWith(
            compareByDescending<Pair<RecipeSearchItem, Int>> { it.second }
                .thenBy { it.first.itemName.length }
                .thenBy { it.first.itemName.lowercase() },
        )
        .take(limit)
        .map { it.first }
        .toList()
}

private fun scoreMealRecipeSuggestion(
    query: String,
    suggestion: RecipeSearchItem,
): Int {
    val name = normalize(suggestion.itemName)
    val tokens = name.split(' ').filter { it.isNotBlank() }

    return when {
        name == query -> 100
        name.startsWith(query) -> 90
        tokens.any { it.startsWith(query) } -> 80
        name.contains(query) -> 70
        else -> 0
    }
}

private fun normalize(value: String): String {
    return value
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}
