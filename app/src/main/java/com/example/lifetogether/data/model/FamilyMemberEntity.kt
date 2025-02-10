package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants

@Entity(
    tableName = Constants.FAMILY_MEMBERS_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = arrayOf("family_id"),
            childColumns = arrayOf("family_id"),
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["family_id"])],
)
data class FamilyMemberEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String = "",

    @ColumnInfo(name = "family_id")
    val familyId: String? = null,

    @ColumnInfo(name = "name")
    val name: String? = null,
)
