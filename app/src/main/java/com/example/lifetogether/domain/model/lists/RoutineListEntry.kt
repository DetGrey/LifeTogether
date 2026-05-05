package com.example.lifetogether.domain.model.lists

import java.util.Date

data class RoutineListEntry(
    override val id: String,
    override val familyId: String,
    override val listId: String,
    override var itemName: String,
    override var lastUpdated: Date,
    override val dateCreated: Date,
    val nextDate: Date,
    val recurrenceUnit: RecurrenceUnit,
    val interval: Int,
    val weekdays: List<Int>,
    val lastCompletedAt: Date? = null,
    val completionCount: Int = 0,
    val imageUrl: String? = null,
) : ListEntry
