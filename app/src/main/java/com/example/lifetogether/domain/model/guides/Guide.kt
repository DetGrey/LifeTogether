package com.example.lifetogether.domain.model.guides

import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.enums.Visibility
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Guide(
    @DocumentId @Transient
    override val id: String = "",
    override val familyId: String = "",
    @get:PropertyName("name")
    @set:PropertyName("name")
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    val description: String = "",
    val visibility: Visibility = Visibility.PRIVATE,
    val ownerUid: String = "",
    val contentVersion: Long = 1,
    val started: Boolean = false,
    val sections: List<GuideSection> = emptyList(),
    val resume: GuideResume? = null,
) : Item
