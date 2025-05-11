package com.example.lifetogether.domain.model.gallery

import android.net.Uri

data class GalleryImageUploadData(
    val uri: Uri,
    val image: GalleryImage,
    val ext: String,
)
