package com.example.lifetogether.domain.model

import com.google.firebase.dataconnect.serializers.DateSerializer
import com.google.firebase.firestore.DocumentId
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Date

@Serializable
data class UserInformation(
    @DocumentId @Transient
    val uid: String? = null,
    val email: String? = null,
    val name: String? = null,
    @Serializable(with = DateSerializer::class)
    val birthday: Date? = null,
    val familyId: String? = null,
    val imageUrl: String? = null,
)
