package com.example.lifetogether.domain.model.lists

import java.util.Date

data class RoutineListEntry(
    override val id: String? = null,
    override val familyId: String = "",
    override val listId: String = "",
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    override val dateCreated: Date = Date(),
    val nextDate: Date? = null,
    val lastCompletedAt: Date? = null,
    val completionCount: Int = 0,
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
    val interval: Int = 1,
    val weekdays: List<Int> = emptyList(),
) : ListEntry
