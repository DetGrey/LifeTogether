package com.example.lifetogether.domain.model.grocery

import com.example.lifetogether.domain.model.Category
import com.google.firebase.firestore.DocumentId

data class GrocerySuggestion(
    @DocumentId @Transient
    val id: String? = null,
    val suggestionName: String = "",
    var category: Category? = null,
)
