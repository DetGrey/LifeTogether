package com.example.lifetogether.domain.model.lists

import java.util.Date

data class ChecklistEntry(
    override val id: String,
    override val familyId: String,
    override val listId: String,
    override var itemName: String,
    val checked: Boolean = false,
    override val lastUpdated: Date = Date(),
    override val dateCreated: Date,
) : ListEntry
