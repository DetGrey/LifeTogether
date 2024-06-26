package com.example.lifetogether.domain.model

import java.util.Date

interface Item {
    val id: String?
    val uid: String
    val itemName: String
    var lastUpdated: Date
    var completed: Boolean
}
