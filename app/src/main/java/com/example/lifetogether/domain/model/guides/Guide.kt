package com.example.lifetogether.domain.model.guides

import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.enums.Visibility
import java.util.Date

data class Guide(
    override val id: String,
    override val familyId: String,
    override var itemName: String,
    override var lastUpdated: Date,
    val description: String,
    val visibility: Visibility,
    val ownerUid: String,
    val contentVersion: Long,
    val sections: List<GuideSection>,
    val started: Boolean = false,
    val resume: GuideResume? = null,
) : Item
