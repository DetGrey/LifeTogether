package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.lists.WishListPriority
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.WISH_LIST_ENTRIES_TABLE)
data class WishListEntryEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    @ColumnInfo(name = "list_id")
    val listId: String,
    @ColumnInfo(name = "item_name")
    val itemName: String,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date,
    @ColumnInfo(name = "date_created")
    val dateCreated: Date,
    @ColumnInfo(name = "is_purchased")
    val isPurchased: Boolean = false,
    val url: String? = null,
    @ColumnInfo(name = "price")
    val price: Double? = null,
    @ColumnInfo(name = "currency_code")
    val currencyCode: String? = null,
    val priority: WishListPriority,
    val notes: String? = null,
)
