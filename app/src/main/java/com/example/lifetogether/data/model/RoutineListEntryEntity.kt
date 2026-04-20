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
    @ColumnInfo(name = "image_data")
    val imageData: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RoutineListEntryEntity) return false

        if (id != other.id) return false
        if (familyId != other.familyId) return false
        if (listId != other.listId) return false
        if (itemName != other.itemName) return false
        if (lastUpdated != other.lastUpdated) return false
        if (dateCreated != other.dateCreated) return false
        if (nextDate != other.nextDate) return false
        if (lastCompletedAt != other.lastCompletedAt) return false
        if (completionCount != other.completionCount) return false
        if (recurrenceUnit != other.recurrenceUnit) return false
        if (interval != other.interval) return false
        if (weekdays != other.weekdays) return false
        if (imageData != null) {
            if (other.imageData == null) return false
            if (!imageData.contentEquals(other.imageData)) return false
        } else if (other.imageData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + familyId.hashCode()
        result = 31 * result + listId.hashCode()
        result = 31 * result + itemName.hashCode()
        result = 31 * result + lastUpdated.hashCode()
        result = 31 * result + dateCreated.hashCode()
        result = 31 * result + (nextDate?.hashCode() ?: 0)
        result = 31 * result + (lastCompletedAt?.hashCode() ?: 0)
        result = 31 * result + completionCount
        result = 31 * result + recurrenceUnit.hashCode()
        result = 31 * result + interval
        result = 31 * result + weekdays.hashCode()
        result = 31 * result + (imageData?.contentHashCode() ?: 0)
        return result
    }
}
