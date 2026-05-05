package com.example.lifetogether.domain.model.family

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable

@Serializable
data class FamilyInformation(
    @DocumentId
    val familyId: String,
    val members: List<FamilyMember> = listOf(),
    val imageUrl: String? = null,
)
