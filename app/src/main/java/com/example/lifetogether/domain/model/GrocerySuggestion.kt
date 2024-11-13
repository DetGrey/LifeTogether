package com.example.lifetogether.domain.model

import com.google.firebase.firestore.DocumentId

data class GrocerySuggestion(
    @DocumentId @Transient
    val id: String? = null,
    val suggestionName: String = "",
    var category: Category? = null,
)
