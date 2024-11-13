package com.example.lifetogether.domain.callback

import com.example.lifetogether.domain.model.GrocerySuggestion

sealed class GrocerySuggestionsListener {
    data class Success(val listItems: List<GrocerySuggestion>) : GrocerySuggestionsListener()
    data class Failure(val message: String) : GrocerySuggestionsListener()
}
