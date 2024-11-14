package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.USER_TABLE)
data class UserEntity(
    @PrimaryKey
    val uid: String = "",
    val email: String? = null,
    val name: String? = null,
    val birthday: Date? = null,
    @ColumnInfo(name = "family_id")
    val familyId: String? = null,
)
