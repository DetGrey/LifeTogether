package com.example.lifetogether.domain.model.sealed

import android.net.Uri
import com.example.lifetogether.domain.model.gallery.GalleryImage

sealed class ImageType {
    data class ProfileImage(val uid: String) : ImageType()
    data class FamilyImage(val familyId: String) : ImageType()
    data class RecipeImage(val familyId: String, val recipeId: String) : ImageType()
    data class GalleryImage(val familyId: String, val albumId: String, val galleryImages: List<Pair<Uri, com.example.lifetogether.domain.model.gallery.GalleryImage>>) : ImageType()
}
