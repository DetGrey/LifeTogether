package com.example.lifetogether.domain.model.gallery

import android.net.Uri

// Data structure to hold information for uploading any media type
// This would be passed to your UploadMediaItemsUseCase
sealed class MediaUploadData {
    abstract val uri: Uri
    abstract val mediaType: GalleryMedia // Using your sealed interface
    abstract val extension: String

    data class ImageUpload(
        override val uri: Uri,
        override val mediaType: GalleryImage,
        override val extension: String,
    ) : MediaUploadData()

    data class VideoUpload(
        override val uri: Uri,
        override val mediaType: GalleryVideo,
        override val extension: String,
    ) : MediaUploadData()
}
