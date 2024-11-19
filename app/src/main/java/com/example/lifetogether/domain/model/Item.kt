package com.example.lifetogether.domain.model

import java.util.Date

interface Item {
    val id: String?
    val familyId: String
    val itemName: String
    var lastUpdated: Date
}

fun Item.toMap(): Map<String, Any?> {
    return mapOf(
        "familyId" to familyId,
        "itemName" to itemName,
        "lastUpdated" to lastUpdated,
    )
}
