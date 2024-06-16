package com.example.lifetogether.domain.model

data class Category(
    val emoji: String,
    val name: String,
    var expanded: Boolean = true,
)
