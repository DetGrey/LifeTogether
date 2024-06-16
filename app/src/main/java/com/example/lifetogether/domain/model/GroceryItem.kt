package com.example.lifetogether.domain.model

import java.util.Date

data class GroceryItem(
    override val uid: String,
    override val username: String,
    override val itemName: String,
    override var lastUpdated: Date,
    override var completed: Boolean,
    var category: Category?,
) : Item
