package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.ROUTINE_LIST_ENTRIES_TABLE)
data class RoutineListEntryEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    @ColumnInfo(name = "list_id")
    val listId: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    @ColumnInfo(name = "date_created")
    val dateCreated: Date = Date(),
    @ColumnInfo(name = "next_date")
    val nextDate: Date? = null,
    @ColumnInfo(name = "last_completed_at")
    val lastCompletedAt: Date? = null,
    @ColumnInfo(name = "completion_count")
    val completionCount: Int = 0,
    @ColumnInfo(name = "recurrence_unit")
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
    val interval: Int = 1,
    val weekdays: List<Int> = emptyList(),
)
