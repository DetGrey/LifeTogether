package com.example.lifetogether.domain.listener

import com.example.lifetogether.domain.model.grocery.GrocerySuggestion

sealed class GrocerySuggestionsListener {
    data class Success(val listItems: List<GrocerySuggestion>) : GrocerySuggestionsListener()
    data class Failure(val message: String) : GrocerySuggestionsListener()
}
