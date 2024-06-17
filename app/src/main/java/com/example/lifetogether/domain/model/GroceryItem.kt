package com.example.lifetogether.domain.model

import java.util.Date

data class GroceryItem(
    override val uid: String = "",
    override val itemName: String = "",
    override var lastUpdated: Date = Date(),
    override var completed: Boolean = false,
    var category: Category? = null,
) : Item
