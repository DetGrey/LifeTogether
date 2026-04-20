package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.USER_LISTS_TABLE)
data class UserListEntity(
    @PrimaryKey
    val id: String = "",
    @ColumnInfo(name = "family_id")
    val familyId: String = "",
    @ColumnInfo(name = "item_name")
    val itemName: String = "",
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    @ColumnInfo(name = "date_created")
    val dateCreated: Date = Date(),
    val type: ListType = ListType.ROUTINE,
    val visibility: Visibility = Visibility.PRIVATE,
    @ColumnInfo(name = "owner_uid")
    val ownerUid: String = "",
)
