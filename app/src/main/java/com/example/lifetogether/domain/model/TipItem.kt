package com.example.lifetogether.domain.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class TipItem(
    @DocumentId @Transient
    override val id: String? = null,
    override val familyId: String = "",
    override val itemName: String = "Tip",
    override var lastUpdated: Date = Date(),
    val amount: Float = 0F,
    val currency: String = "DKK",
    val date: Date = Date(),
) : Item
