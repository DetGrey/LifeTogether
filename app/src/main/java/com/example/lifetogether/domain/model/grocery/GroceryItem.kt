package com.example.lifetogether.domain.model.grocery

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.google.firebase.firestore.DocumentId
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY
import java.util.Date

data class GroceryItem(
    @DocumentId @Transient
    override val id: String = "",
    override val familyId: String = "",
    override val itemName: String = "",
    override var lastUpdated: Date = Date(),
    override var completed: Boolean = false,
    var category: Category = UNCATEGORIZED_CATEGORY,
    var approxPrice: Float? = null,
) : CompletableItem
