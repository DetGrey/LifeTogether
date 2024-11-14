package com.example.lifetogether.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants

@Entity(tableName = Constants.CATEGORY_TABLE)
data class CategoryEntity(
    val emoji: String = "",
    @PrimaryKey
    val name: String = "",
)
