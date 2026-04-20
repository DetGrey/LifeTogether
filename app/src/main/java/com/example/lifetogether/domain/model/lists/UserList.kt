package com.example.lifetogether.domain.model.lists

import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.enums.Visibility
import java.util.Date

data class UserList(
    override val id: String? = null,
    override val familyId: String = "",
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    val dateCreated: Date = Date(),
    val type: ListType = ListType.ROUTINE,
    val visibility: Visibility = Visibility.PRIVATE,
    val ownerUid: String = "",
) : Item
