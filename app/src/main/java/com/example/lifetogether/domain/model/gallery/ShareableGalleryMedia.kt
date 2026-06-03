package com.example.lifetogether.domain.model.gallery

import android.net.Uri

data class ShareableGalleryMedia(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
)
