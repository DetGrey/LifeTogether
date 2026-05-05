package com.example.lifetogether.domain.model.gallery

import android.net.Uri
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.enums.MediaType
import java.util.Date

sealed interface GalleryMedia : Item {
    override var id: String
    override val familyId: String
    override var itemName: String
    override var lastUpdated: Date
    val albumId: String
    val dateCreated: Date
    val mediaType: MediaType
    val mediaUrl: String?
    val mediaUri: Uri?
}

data class GalleryImage(
    override var id: String,
    override val familyId: String,
    override var itemName: String,
    override var lastUpdated: Date,
    override val albumId: String,
    override val dateCreated: Date,
    override val mediaType: MediaType = MediaType.IMAGE,
    override val mediaUrl: String? = null,
    override val mediaUri: Uri? = null,
) : GalleryMedia

data class GalleryVideo(
    override var id: String,
    override val familyId: String,
    override var itemName: String,
    override var lastUpdated: Date,
    override val albumId: String,
    override val dateCreated: Date,
    override val mediaType: MediaType = MediaType.VIDEO,
    override val mediaUrl: String? = null,
    override val mediaUri: Uri? = null,
    val duration: Long? = null,
) : GalleryMedia
