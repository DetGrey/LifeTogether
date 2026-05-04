package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.NOTE_LIST_ENTRIES_TABLE)
data class NoteEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    @ColumnInfo(name = "list_id")
    val listId: String,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    @ColumnInfo(name = "markdown_body")
    val markdownBody: String,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
    @ColumnInfo(name = "date_created")
    val dateCreated: Date,
)
