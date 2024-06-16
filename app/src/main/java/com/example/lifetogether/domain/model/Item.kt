package com.example.lifetogether.domain.model

import java.util.Date

interface Item {
    val uid: String
    val username: String
    val itemName: String
    var lastUpdated: Date
    var checked: Boolean
}
