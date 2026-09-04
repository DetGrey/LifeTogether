package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.model.beach.BeachAlbum
import com.example.lifetogether.domain.model.beach.BeachMedia
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class BeachFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "BeachFirestoreDS"
    }

    fun albumsSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.BEACH_ALBUMS_TABLE).whereEqualTo("familyId", familyId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = mapFirestoreDocuments(
                    tag = TAG,
                    collectionName = Constants.BEACH_ALBUMS_TABLE,
                    entityName = "BeachAlbum",
                    documents = snapshot.documents,
                ) { doc ->
                    doc.toObject(BeachAlbumDto::class.java)?.toDomain(doc.id, familyId)
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    fun mediaSnapshotListener(familyId: String, albumId: String) = callbackFlow {
        val ref = db.collection(Constants.BEACH_MEDIA_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("beachAlbumId", albumId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = mapFirestoreDocuments(
                    tag = TAG,
                    collectionName = Constants.BEACH_MEDIA_TABLE,
                    entityName = "BeachMedia",
                    documents = snapshot.documents,
                ) { doc ->
                    doc.toObject(BeachMediaDto::class.java)?.toDomain(doc.id, familyId)
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    fun allFamilyMediaSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.BEACH_MEDIA_TABLE)
            .whereEqualTo("familyId", familyId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = mapFirestoreDocuments(
                    tag = TAG,
                    collectionName = Constants.BEACH_MEDIA_TABLE,
                    entityName = "BeachMedia",
                    documents = snapshot.documents,
                ) { doc ->
                    doc.toObject(BeachMediaDto::class.java)?.toDomain(doc.id, familyId)
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun upsertBeachAlbum(familyId: String, album: BeachAlbum): Result<Unit, AppError> = appResultOfSuspend {
        val dto = BeachAlbumDto(
            id = album.id,
            familyId = familyId,
            name = album.name,
            count = album.count,
            createdAt = album.createdAt,
            lastUpdated = album.lastUpdated,
        )
        db.collection(Constants.BEACH_ALBUMS_TABLE).document(album.id).set(dto).await()
    }

    suspend fun deleteBeachAlbum(albumId: String): Result<Unit, AppError> = appResultOfSuspend {
        db.collection(Constants.BEACH_ALBUMS_TABLE).document(albumId).delete().await()
    }

    suspend fun upsertBeachMedia(familyId: String, mediaItems: List<BeachMedia>): Result<Unit, AppError> = appResultOfSuspend {
        val batch = db.batch()
        val mediaCollection = db.collection(Constants.BEACH_MEDIA_TABLE)
        mediaItems.forEach { item ->
            val dto = BeachMediaDto(
                id = item.id,
                familyId = familyId,
                beachAlbumId = item.beachAlbumId,
                url = item.url,
                storagePath = item.storagePath,
                createdAt = item.createdAt,
                lastUpdated = item.lastUpdated,
            )
            val docRef = mediaCollection.document(item.id)
            batch.set(docRef, dto)
        }
        batch.commit().await()
    }

    suspend fun deleteBeachMedia(mediaIds: List<String>): Result<Unit, AppError> = appResultOfSuspend {
        val batch = db.batch()
        val mediaCollection = db.collection(Constants.BEACH_MEDIA_TABLE)
        mediaIds.forEach { id ->
            val docRef = mediaCollection.document(id)
            batch.delete(docRef)
        }
        batch.commit().await()
    }
}

private data class BeachAlbumDto(
    @DocumentId val id: String? = null,
    val familyId: String = "",
    val name: String = "",
    val count: Int = 0,
    val createdAt: Date = Date(),
    val lastUpdated: Date = Date(),
) {
    fun toDomain(documentId: String, familyIdArg: String): BeachAlbum = BeachAlbum(
        id = id ?: documentId,
        familyId = if (familyId.isNotBlank()) familyId else familyIdArg,
        name = name,
        count = count,
        createdAt = createdAt,
        lastUpdated = lastUpdated,
    )
}

private data class BeachMediaDto(
    @DocumentId val id: String? = null,
    val familyId: String = "",
    val beachAlbumId: String = "",
    val url: String = "",
    val storagePath: String = "",
    val createdAt: Date = Date(),
    val lastUpdated: Date = Date(),
) {
    fun toDomain(documentId: String, familyIdArg: String): BeachMedia = BeachMedia(
        id = id ?: documentId,
        familyId = if (familyId.isNotBlank()) familyId else familyIdArg,
        beachAlbumId = beachAlbumId,
        url = url,
        storagePath = storagePath,
        createdAt = createdAt,
        lastUpdated = lastUpdated,
    )
}
