package com.example.lifetogether.domain.model.sealed

sealed class ImageType {
    data class ProfileImage(val uid: String) : ImageType()
    data class FamilyImage(val familyId: String) : ImageType()
    data class RecipeImage(val familyId: String, val recipeId: String) : ImageType()
}
