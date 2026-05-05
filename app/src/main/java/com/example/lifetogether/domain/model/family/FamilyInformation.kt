package com.example.lifetogether.domain.model.family

import kotlinx.serialization.Serializable

@Serializable
data class FamilyInformation(
    val familyId: String,
    val members: List<FamilyMember> = listOf(),
    val imageUrl: String? = null,
)
