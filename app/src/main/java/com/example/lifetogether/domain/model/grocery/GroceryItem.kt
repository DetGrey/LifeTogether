package com.example.lifetogether.domain.model.grocery

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class GroceryItem(
    @DocumentId @Transient
    override val id: String? = null,
    override val familyId: String = "",
    override val itemName: String = "",
    override var lastUpdated: Date = Date(),
    override var completed: Boolean = false,
    var category: Category? = null,
) : CompletableItem
