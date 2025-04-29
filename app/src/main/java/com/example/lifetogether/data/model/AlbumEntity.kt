package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.ALBUMS_TABLE)
data class AlbumEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    var count: Int = 0,
)
