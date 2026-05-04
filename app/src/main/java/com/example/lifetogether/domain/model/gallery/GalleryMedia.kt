package com.example.lifetogether.domain.model.gallery

import android.net.Uri
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.enums.MediaType
import com.google.firebase.firestore.DocumentId
import java.util.Date

sealed interface GalleryMedia : Item {
    override var id: String
    override val familyId: String
    override var itemName: String
    override var lastUpdated: Date
    val albumId: String
    val dateCreated: Date?
    val mediaType: MediaType
    val mediaUrl: String?
    val mediaUri: Uri?
}

data class GalleryImage(
    @DocumentId @Transient
    override var id: String = "",
    override val familyId: String = "",
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    override val albumId: String = "",
    override val dateCreated: Date? = null,
    override val mediaType: MediaType = MediaType.IMAGE,
    override val mediaUrl: String? = null,
    @Transient
    override val mediaUri: Uri? = null,
) : GalleryMedia

data class GalleryVideo(
    @DocumentId @Transient
    override var id: String = "",
    override val familyId: String = "",
    override var itemName: String = "",
    override var lastUpdated: Date = Date(),
    override val albumId: String = "",
    override val dateCreated: Date? = null,
    override val mediaType: MediaType = MediaType.VIDEO,
    override val mediaUrl: String? = null,
    @Transient
    override val mediaUri: Uri? = null,
    val duration: Long? = null,
) : GalleryMedia
