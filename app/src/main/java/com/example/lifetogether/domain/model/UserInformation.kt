package com.example.lifetogether.domain.model

import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Transient
import java.util.Date

data class UserInformation(
    @DocumentId @Transient
    val uid: String? = null,
    val email: String? = null,
    val name: String? = null,
    val birthday: Date? = null,
    val familyId: String? = null,
    val imageUrl: String? = null,
)
