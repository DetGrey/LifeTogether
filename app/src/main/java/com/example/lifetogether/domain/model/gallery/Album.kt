package com.example.lifetogether.domain.model.gallery

import com.example.lifetogether.domain.model.Item
import java.util.Date

data class Album(
    override var id: String,
    override val familyId: String,
    override var itemName: String,
    override var lastUpdated: Date,
    var count: Int = 0,
) : Item
