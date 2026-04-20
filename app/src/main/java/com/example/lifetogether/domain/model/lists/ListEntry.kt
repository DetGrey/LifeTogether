package com.example.lifetogether.domain.model.lists

import com.example.lifetogether.domain.model.Item
import java.util.Date

interface ListEntry : Item {
    val listId: String
    val dateCreated: Date
}
