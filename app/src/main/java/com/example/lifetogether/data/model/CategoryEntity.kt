package com.example.lifetogether.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    val emoji: String = "",
    @PrimaryKey
    val name: String = "",
)
