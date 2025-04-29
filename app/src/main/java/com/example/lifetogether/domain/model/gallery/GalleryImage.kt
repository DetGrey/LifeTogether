package com.example.lifetogether.domain.model.gallery

import com.example.lifetogether.domain.model.Item
import com.google.firebase.firestore.DocumentId
import java.util.Date

data class GalleryImage(
    @DocumentId @Transient
    override var id: String? = null,
    override val familyId: String = "",
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    val albumId: String = "",
    val dateCreated: Date? = null,
    val imageUrl: String? = null,
) : Item