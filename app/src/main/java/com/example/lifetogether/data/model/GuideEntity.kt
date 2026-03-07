package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.guides.GuideResume
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideVisibility
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.GUIDES_TABLE)
data class GuideEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    val description: String = "",
    val visibility: GuideVisibility = GuideVisibility.PRIVATE,
    @ColumnInfo(name = "owner_uid")
    val ownerUid: String = "",
    val started: Boolean = false,
    val sections: List<GuideSection> = emptyList(),
    val resume: GuideResume? = null,
)
