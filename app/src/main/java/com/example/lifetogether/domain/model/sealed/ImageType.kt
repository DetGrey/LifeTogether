package com.example.lifetogether.domain.model.sealed

sealed class ImageType {
    data class ProfileImage(val uid: String) : ImageType()
    data class FamilyImage(val familyId: String) : ImageType()
}
