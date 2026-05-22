package com.example.lifetogether.ui.feature.groceryList.components

import com.example.lifetogether.domain.model.grocery.GrocerySuggestion

private const val DEFAULT_MIN_QUERY_LENGTH = 2
private const val DEFAULT_LIMIT = 10

fun searchGrocerySuggestions(
    query: String,
    suggestions: List<GrocerySuggestion>,
    limit: Int = DEFAULT_LIMIT,
    minQueryLength: Int = DEFAULT_MIN_QUERY_LENGTH,
): List<GrocerySuggestion> {
    val normalizedQuery = normalizeSearchText(query)
    if (normalizedQuery.length < minQueryLength) return emptyList()

    return suggestions
        .mapNotNull { suggestion ->
            val score = scoreSuggestion(normalizedQuery, suggestion)
            if (score > 0) {
                suggestion to score
            } else {
                null
            }
        }
        .sortedWith(
            compareByDescending<Pair<GrocerySuggestion, Int>> { it.second }
                .thenBy { it.first.suggestionName.length }
                .thenBy { it.first.suggestionName.lowercase() },
        )
        .take(limit)
        .map { it.first }
}

private fun scoreSuggestion(
    normalizedQuery: String,
    suggestion: GrocerySuggestion,
): Int {
    val normalizedName = normalizeSearchText(suggestion.suggestionName)
    val normalizedCategory = normalizeSearchText(suggestion.category.name)
    val nameTokens = normalizedName.split(' ').filter { it.isNotBlank() }
    val categoryTokens = normalizedCategory.split(' ').filter { it.isNotBlank() }

    return when {
        normalizedName == normalizedQuery -> 100
        normalizedName.startsWith(normalizedQuery) -> 90
        nameTokens.any { it.startsWith(normalizedQuery) } -> 80
        normalizedCategory.startsWith(normalizedQuery) -> 70
        categoryTokens.any { it.startsWith(normalizedQuery) } -> 65
        normalizedName.contains(normalizedQuery) -> 60
        normalizedCategory.contains(normalizedQuery) -> 50
        else -> 0
    }
}

private fun normalizeSearchText(value: String): String {
    return value
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}
