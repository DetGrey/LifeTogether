package com.example.lifetogether.domain.model.lists

import java.util.Date

data class ChecklistEntry(
    override val id: String,
    override val familyId: String,
    override val listId: String,
    override var itemName: String,
    val isChecked: Boolean = false,
    override var lastUpdated: Date,
    override val dateCreated: Date,
) : ListEntry
