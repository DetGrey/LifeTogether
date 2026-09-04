package com.example.lifetogether.domain.model.beach

import java.util.Date

data class BeachAlbum(
    val id: String,
    val familyId: String,
    val name: String,
    val count: Int = 0,
    val createdAt: Date = Date(),
    val lastUpdated: Date = Date(),
)
