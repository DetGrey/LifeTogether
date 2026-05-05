package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.PropertyName
import kotlin.jvm.Transient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class GalleryFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "GalleryFirestoreDS"
    }
    fun albumsSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.ALBUMS_TABLE).whereEqualTo("familyId", familyId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(AlbumDto::class.java)?.toDomain(doc.id)
                    } catch (throwable: Throwable) {
                        Log.e(TAG, "Failed parsing album ${doc.id}", throwable)
                        null
                    }
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    fun galleryMediaSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.GALLERY_MEDIA_TABLE).whereEqualTo("familyId", familyId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(GalleryMediaDto::class.java)?.toDomain(document.id)
                    } catch (throwable: Throwable) {
                        Log.e(TAG, "Error parsing document ${document.id} to GalleryMedia", throwable)
                        null
                    }
                }
                trySend(Result.Success(ListSnapshot(items, snapshot.metadata.isFromCache))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveAlbum(album: Album): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.ALBUMS_TABLE).add(album.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun updateAlbum(album: Album): Result<Unit, AppError> {
        val id = album.id
        return appResultOfSuspend {
            db.collection(Constants.ALBUMS_TABLE).document(id).set(album.toDto().toFirestoreMap(), SetOptions.merge()).await()
        }
    }

    suspend fun deleteAlbum(albumId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.ALBUMS_TABLE).document(albumId).delete().await()
        }
    }

    suspend fun deleteGalleryMedia(mediaIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            mediaIds.forEach { id -> batch.delete(db.collection(Constants.GALLERY_MEDIA_TABLE).document(id)) }
            batch.commit().await()
        }
    }

    suspend fun updateAlbumCount(albumId: String, increment: Int): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.ALBUMS_TABLE).document(albumId)
                .update("count", FieldValue.increment(increment.toDouble())).await()
        }
    }

    suspend fun moveMediaToAlbum(mediaIdList: Set<String>, newAlbumId: String, oldAlbumId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            mediaIdList.forEach { id ->
                batch.update(db.collection(Constants.GALLERY_MEDIA_TABLE).document(id), "albumId", newAlbumId)
            }
            batch.update(db.collection(Constants.ALBUMS_TABLE).document(newAlbumId), "count", FieldValue.increment(mediaIdList.size.toDouble()))
            batch.update(db.collection(Constants.ALBUMS_TABLE).document(oldAlbumId), "count", FieldValue.increment(-mediaIdList.size.toDouble()))
            batch.commit().await()
        }
    }

    suspend fun saveGalleryMediaMetaData(galleryMedia: List<GalleryMedia>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            val collectionRef = db.collection(Constants.GALLERY_MEDIA_TABLE)
            galleryMedia.forEach { media ->
                val docRef = collectionRef.document()
                batch.set(docRef, media.toDto().toFirestoreMap())
            }
            batch.commit().await()
        }
    }
}

private data class AlbumDto(
    @DocumentId @Transient
    val id: String? = null,
    val familyId: String? = null,
    @get:PropertyName("item_name")
    val itemName: String? = null,
    @get:PropertyName("last_updated")
    val lastUpdated: Date? = null,
    val count: Int? = null,
) {
    fun toDomain(documentId: String): Album? {
        val familyIdValue = familyId?.takeIf { it.isNotBlank() } ?: return null
        val itemNameValue = itemName?.takeIf { it.isNotBlank() } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        return Album(
            id = documentId,
            familyId = familyIdValue,
            itemName = itemNameValue,
            lastUpdated = lastUpdatedValue,
            count = count ?: 0,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "familyId" to familyId,
        "item_name" to itemName,
        "last_updated" to lastUpdated,
        "count" to count,
    )
}

private data class GalleryMediaDto(
    @DocumentId @Transient
    val id: String? = null,
    val familyId: String? = null,
    @get:PropertyName("item_name")
    val itemName: String? = null,
    @get:PropertyName("last_updated")
    val lastUpdated: Date? = null,
    @get:PropertyName("album_id")
    val albumId: String? = null,
    @get:PropertyName("date_created")
    val dateCreated: Date? = null,
    @get:PropertyName("media_type")
    val mediaType: String? = null,
    @get:PropertyName("media_uri")
    val mediaUri: String? = null,
    val thumbnail: ByteArray? = null,
    @get:PropertyName("video_duration")
    val videoDuration: Long? = null,
) {
    fun toDomain(documentId: String): GalleryMedia? {
        val familyIdValue = familyId?.takeIf { it.isNotBlank() } ?: return null
        val itemNameValue = itemName?.takeIf { it.isNotBlank() } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        val albumIdValue = albumId?.takeIf { it.isNotBlank() } ?: return null
        val dateCreatedValue = dateCreated ?: return null
        val mediaTypeValue = MediaType.fromValue(mediaType) ?: return null
        return when (mediaTypeValue) {
            MediaType.IMAGE -> GalleryImage(
                id = documentId,
                familyId = familyIdValue,
                itemName = itemNameValue,
                lastUpdated = lastUpdatedValue,
                albumId = albumIdValue,
                dateCreated = dateCreatedValue,
                mediaType = mediaTypeValue,
                mediaUrl = mediaUri,
            )

            MediaType.VIDEO -> GalleryVideo(
                id = documentId,
                familyId = familyIdValue,
                itemName = itemNameValue,
                lastUpdated = lastUpdatedValue,
                albumId = albumIdValue,
                dateCreated = dateCreatedValue,
                mediaType = mediaTypeValue,
                mediaUrl = mediaUri,
                duration = videoDuration,
            )
        }
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "familyId" to familyId,
        "item_name" to itemName,
        "last_updated" to lastUpdated,
        "album_id" to albumId,
        "date_created" to dateCreated,
        "media_type" to mediaType,
        "media_uri" to mediaUri,
        "thumbnail" to thumbnail,
        "video_duration" to videoDuration,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GalleryMediaDto

        if (videoDuration != other.videoDuration) return false
        if (id != other.id) return false
        if (familyId != other.familyId) return false
        if (itemName != other.itemName) return false
        if (lastUpdated != other.lastUpdated) return false
        if (albumId != other.albumId) return false
        if (dateCreated != other.dateCreated) return false
        if (mediaType != other.mediaType) return false
        if (mediaUri != other.mediaUri) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = videoDuration?.hashCode() ?: 0
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (familyId?.hashCode() ?: 0)
        result = 31 * result + (itemName?.hashCode() ?: 0)
        result = 31 * result + (lastUpdated?.hashCode() ?: 0)
        result = 31 * result + (albumId?.hashCode() ?: 0)
        result = 31 * result + (dateCreated?.hashCode() ?: 0)
        result = 31 * result + (mediaType?.hashCode() ?: 0)
        result = 31 * result + (mediaUri?.hashCode() ?: 0)
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        return result
    }
}

private fun Album.toDto(): AlbumDto = AlbumDto(
    id = id,
    familyId = familyId,
    itemName = itemName,
    lastUpdated = lastUpdated,
    count = count,
)

private fun GalleryMedia.toDto(): GalleryMediaDto = GalleryMediaDto(
    id = id,
    familyId = familyId,
    itemName = itemName,
    lastUpdated = lastUpdated,
    albumId = albumId,
    dateCreated = dateCreated,
    mediaType = mediaType.value,
    mediaUri = mediaUrl,
    thumbnail = null,
    videoDuration = (this as? GalleryVideo)?.duration,
)
