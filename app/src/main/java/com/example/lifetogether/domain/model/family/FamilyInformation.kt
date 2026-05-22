package com.example.lifetogether.domain.model.family

import java.util.Date

data class FamilyInformation(
    val familyId: String,
    val members: List<FamilyMember> = listOf(),
    val lastUpdated: Date = Date(),
    val imageUrl: String? = null,
    val togetherSince: Date? = null,
)
