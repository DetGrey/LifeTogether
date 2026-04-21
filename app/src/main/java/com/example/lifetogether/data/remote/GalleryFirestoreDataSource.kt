package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
                    runCatching { doc.toObject(Album::class.java)?.copy(id = doc.id) }
                        .onFailure { Log.e(TAG, "Failed parsing album ${doc.id}", it) }
                        .getOrNull()
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
                val items = mutableListOf<GalleryMedia>()
                for (document in snapshot.documents) {
                    try {
                        val mediaType = document.getString("mediaType")
                        val item = when (mediaType?.lowercase()) {
                            "image" -> document.toObject(GalleryImage::class.java) as GalleryMedia
                            "video" -> document.toObject(GalleryVideo::class.java) as GalleryMedia
                            else -> null
                        }
                        item?.let { items.add(it) }
                    } catch (parseEx: Exception) {
                        Log.e(TAG, "Error parsing document ${document.id} to GalleryMedia", parseEx)
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
        return try {
            val doc = db.collection(Constants.ALBUMS_TABLE).add(album).await()
            Result.Success(doc.id)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun updateAlbum(album: Album): Result<Unit, AppError> {
        return try {
            val id = album.id ?: return Result.Failure(AppErrors.validation("Missing album id"))
            db.collection(Constants.ALBUMS_TABLE).document(id).set(album, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun deleteAlbum(albumId: String): Result<Unit, AppError> {
        return try {
            db.collection(Constants.ALBUMS_TABLE).document(albumId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun deleteGalleryMedia(mediaIds: List<String>): Result<Unit, AppError> {
        return try {
            val batch = db.batch()
            mediaIds.forEach { id -> batch.delete(db.collection(Constants.GALLERY_MEDIA_TABLE).document(id)) }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun updateAlbumCount(albumId: String, increment: Int): Result<Unit, AppError> {
        return try {
            db.collection(Constants.ALBUMS_TABLE).document(albumId)
                .update("count", FieldValue.increment(increment.toDouble())).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun moveMediaToAlbum(mediaIdList: Set<String>, newAlbumId: String, oldAlbumId: String): Result<Unit, AppError> {
        return try {
            val batch = db.batch()
            mediaIdList.forEach { id ->
                batch.update(db.collection(Constants.GALLERY_MEDIA_TABLE).document(id), "albumId", newAlbumId)
            }
            batch.update(db.collection(Constants.ALBUMS_TABLE).document(newAlbumId), "count", FieldValue.increment(mediaIdList.size.toDouble()))
            batch.update(db.collection(Constants.ALBUMS_TABLE).document(oldAlbumId), "count", FieldValue.increment(-mediaIdList.size.toDouble()))
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun saveGalleryMediaMetaData(galleryMedia: List<GalleryMedia>): Result<Unit, AppError> {
        return try {
            val batch = db.batch()
            val collectionRef = db.collection(Constants.GALLERY_MEDIA_TABLE)
            galleryMedia.forEach { media ->
                val docRef = collectionRef.document()
                batch.set(docRef, media)
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }
}
