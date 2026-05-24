package com.example.lifetogether.domain.model.lists

import java.util.Date

data class WishListEntry(
    override val id: String,
    override val familyId: String,
    override val listId: String,
    override var itemName: String,
    override val lastUpdated: Date = Date(),
    override val dateCreated: Date,
    val isPurchased: Boolean = false,
    val url: String? = null,
    val price: Double? = null,
    val currencyCode: String? = null,
    val priority: WishListPriority,
    val notes: String? = null,
) : ListEntry
