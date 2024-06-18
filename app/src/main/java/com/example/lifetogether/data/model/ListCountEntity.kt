package com.example.lifetogether.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Assuming you have an Entity for your lists that includes a count
@Entity(tableName = "list_counts")
data class ListCountEntity(
    @PrimaryKey val listName: String,
    val itemCount: Int,
)
