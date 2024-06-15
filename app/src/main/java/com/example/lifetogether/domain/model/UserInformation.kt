package com.example.lifetogether.domain.model

import com.google.type.DateTime

data class UserInformation(
    val uid: String,
    val email: String? = null, // TODO remove null and only add this data class after fetching from db
    val name: String? = null,
    val birthday: DateTime? = null,
    val familyId: String? = null,
)
