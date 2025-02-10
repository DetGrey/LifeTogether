package com.example.lifetogether.domain.model.family

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class FamilyInformation(
    @DocumentId @Transient
    val familyId: String? = null,
    val members: List<FamilyMember>? = null,
    val imageUrl: String? = null,
)
