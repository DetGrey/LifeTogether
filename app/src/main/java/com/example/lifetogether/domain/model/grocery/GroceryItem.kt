package com.example.lifetogether.domain.model.grocery

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import java.util.Date

data class GroceryItem(
    override val id: String,
    override val familyId: String,
    override val itemName: String,
    override val lastUpdated: Date = Date(),
    var category: Category,
    override var completed: Boolean = false,
    var approxPrice: Float? = null,
) : CompletableItem
