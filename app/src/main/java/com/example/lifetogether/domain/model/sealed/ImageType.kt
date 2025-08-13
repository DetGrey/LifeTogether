package com.example.lifetogether.domain.model.sealed

import com.example.lifetogether.domain.model.gallery.MediaUploadData

sealed class ImageType {
    data class ProfileImage(val uid: String) : ImageType()
    data class FamilyImage(val familyId: String) : ImageType()
    data class RecipeImage(val familyId: String, val recipeId: String) : ImageType()
    data class GalleryMedia(val familyId: String, val albumId: String, val galleryMediaUploadData: MediaUploadData?) : ImageType()
}
