package com.example.lifetogether.domain.model.family

import kotlinx.serialization.Serializable

@Serializable
data class FamilyMember(
    val uid: String? = null,
    val name: String? = null,
)
