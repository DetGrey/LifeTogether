package com.example.lifetogether.domain.model

import java.util.Date

data class UserInformation(
    val uid: String,
    val email: String,
    val name: String,
    val lastUpdated: Date = Date(),
    val birthday: Date? = null,
    val familyId: String? = null,
    val imageUrl: String? = null,
)
