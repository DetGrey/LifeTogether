package com.example.lifetogether.domain.model

import java.util.Date

data class Category(
    val emoji: String,
    val name: String,
    val lastUpdated: Date = Date(),
)
