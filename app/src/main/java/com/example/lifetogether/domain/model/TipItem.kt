package com.example.lifetogether.domain.model

import java.util.Date

data class TipItem(
    override val id: String,
    override val familyId: String,
    override val itemName: String,
    override var lastUpdated: Date,
    val amount: Float,
    val date: Date,
    val currency: String = "DKK",
) : Item
