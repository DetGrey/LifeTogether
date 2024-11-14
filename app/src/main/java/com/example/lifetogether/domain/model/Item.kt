package com.example.lifetogether.domain.model

import java.util.Date

interface Item {
    val id: String?
    val familyId: String
    val itemName: String
    var lastUpdated: Date
}
