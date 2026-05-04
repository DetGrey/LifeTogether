package com.example.lifetogether.domain.model.lists

import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.enums.Visibility
import java.util.Date

data class UserList(
    override val id: String,
    override val familyId: String,
    override var itemName: String,
    override var lastUpdated: Date,
    val dateCreated: Date,
    val type: ListType,
    val visibility: Visibility,
    val ownerUid: String,
) : Item
