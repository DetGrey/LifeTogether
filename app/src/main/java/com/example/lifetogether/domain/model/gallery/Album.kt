package com.example.lifetogether.domain.model.gallery

import com.example.lifetogether.domain.model.Item
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class Album(
    @DocumentId @Transient
    override var id: String? = null,
    override val familyId: String = "",
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    var count: Int = 0,
) : Item
