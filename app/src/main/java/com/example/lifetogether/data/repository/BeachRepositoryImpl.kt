package com.example.lifetogether.data.repository

import android.content.Context
import android.net.Uri
import com.example.lifetogether.data.local.source.BeachAlbumLocalDataSource
import com.example.lifetogether.data.local.source.BeachMediaLocalDataSource
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.model.BeachAlbumEntity
import com.example.lifetogether.data.model.BeachMediaEntity
import com.example.lifetogether.data.remote.BeachFirestoreDataSource
import com.example.lifetogether.data.repository.internal.stampNow
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.model.beach.BeachAlbum
import com.example.lifetogether.domain.model.beach.BeachMedia
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.BeachRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeachRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val beachAlbumLocalDataSource: BeachAlbumLocalDataSource,
    private val beachMediaLocalDataSource: BeachMediaLocalDataSource,
    private val beachFirestoreDataSource: BeachFirestoreDataSource,
    private val storageDataSource: StorageDataSource,
) : BeachRepository {

    override fun observeBeachAlbums(familyId: String): Flow<Result<List<BeachAlbum>, AppError>> {
        return beachAlbumLocalDataSource.observeBeachAlbums(familyId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun observeBeachAlbumThumbnails(familyId: String): Flow<Map<String, ByteArray>> {
        return beachMediaLocalDataSource.observeAlbumThumbnails(familyId)
    }

    override fun observeBeachAlbumById(familyId: String, albumId: String): Flow<Result<BeachAlbum, AppError>> {
        return beachAlbumLocalDataSource.observeBeachAlbumById(familyId, albumId).map { entity ->
            if (entity != null) {
                Result.Success(entity.toDomain())
            } else {
                Result.Failure(AppError.NotFound("Beach album not found"))
            }
        }
    }

    override fun observeBeachMedia(familyId: String, albumId: String): Flow<Result<List<BeachMedia>, AppError>> {
        return beachMediaLocalDataSource.observeBeachMedia(familyId, albumId).map { entities ->
            Result.Success(entities.map { it.toDomain() })
        }
    }

    override fun syncBeachAlbumsFromRemote(familyId: String): Flow<Result<Unit, AppError>> = channelFlow {
        launch {
            beachFirestoreDataSource.albumsSnapshotListener(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        beachAlbumLocalDataSource.updateBeachAlbums(familyId, result.data.items)
                        send(Result.Success(Unit))
                    }
                    is Result.Failure -> send(Result.Failure(result.error))
                }
            }
        }
        launch {
            beachFirestoreDataSource.allFamilyMediaSnapshotListener(familyId).collect { result ->
                if (result is Result.Success) {
                    beachMediaLocalDataSource.syncFamilyBeachMedia(familyId, result.data.items)
                    downloadMissingThumbnails(familyId)
                }
            }
        }
    }

    override fun syncBeachMediaFromRemote(familyId: String, albumId: String): Flow<Result<Unit, AppError>> {
        return beachFirestoreDataSource.mediaSnapshotListener(familyId, albumId).map { result ->
            when (result) {
                is Result.Success -> {
                    beachMediaLocalDataSource.updateBeachMedia(familyId, albumId, result.data.items)
                    downloadMissingThumbnails(familyId)
                    Result.Success(Unit)
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    private suspend fun downloadMissingThumbnails(familyId: String) {
        val missingItems = beachMediaLocalDataSource.getItemsWithMissingThumbnails(familyId)
        if (missingItems.isEmpty()) return

        missingItems.forEach { entity ->
            if (entity.url.isNotBlank()) {
                val fetchResult = storageDataSource.fetchImageByteArray(entity.url)
                if (fetchResult is Result.Success) {
                    beachMediaLocalDataSource.markMediaDownloadSuccess(entity.id, fetchResult.data)
                } else if (fetchResult is Result.Failure) {
                    beachMediaLocalDataSource.markMediaDownloadFailure(
                        id = entity.id,
                        error = fetchResult.error.toString(),
                        attemptDate = Date(),
                    )
                }
            }
        }
    }

    override suspend fun retryBeachMediaDownloads(
        mediaIds: List<String>,
        familyId: String,
    ): Result<Unit, AppError> {
        if (mediaIds.isEmpty()) return Result.Success(Unit)

        beachMediaLocalDataSource.markMediaDownloadsPending(familyId, mediaIds)
        downloadMissingThumbnails(familyId)
        return Result.Success(Unit)
    }

    override suspend fun createBeachAlbum(familyId: String, name: String): Result<BeachAlbum, AppError> {
        val now = Date()
        val album = BeachAlbum(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            name = name,
            count = 0,
            createdAt = now,
            lastUpdated = now,
        )

        val entity = BeachAlbumEntity(
            id = album.id,
            familyId = familyId,
            name = album.name,
            count = album.count,
            createdAt = album.createdAt,
            lastUpdated = album.lastUpdated,
        )

        // Local-first optimistic write
        beachAlbumLocalDataSource.upsertBeachAlbum(entity)

        // Remote Firestore write
        val remoteResult = beachFirestoreDataSource.upsertBeachAlbum(familyId, album)
        if (remoteResult is Result.Failure) {
            // Rollback optimistic write on failure
            beachAlbumLocalDataSource.deleteBeachAlbum(familyId, album.id)
            return Result.Failure(remoteResult.error)
        }

        return Result.Success(album)
    }

    override suspend fun updateBeachAlbum(album: BeachAlbum): Result<Unit, AppError> {
        val now = Date()
        val stampedAlbum = album.stampNow(now)
        val entity = BeachAlbumEntity(
            id = stampedAlbum.id,
            familyId = stampedAlbum.familyId,
            name = stampedAlbum.name,
            count = stampedAlbum.count,
            createdAt = stampedAlbum.createdAt,
            lastUpdated = stampedAlbum.lastUpdated,
        )

        beachAlbumLocalDataSource.upsertBeachAlbum(entity)
        return beachFirestoreDataSource.upsertBeachAlbum(stampedAlbum.familyId, stampedAlbum)
    }

    override suspend fun deleteBeachAlbum(familyId: String, albumId: String): Result<Unit, AppError> {
        beachAlbumLocalDataSource.deleteBeachAlbum(familyId, albumId)
        beachMediaLocalDataSource.deleteBeachMediaByAlbumId(familyId, albumId)
        return beachFirestoreDataSource.deleteBeachAlbum(albumId)
    }

    override suspend fun deleteBeachAlbums(familyId: String, albumIds: List<String>): Result<Unit, AppError> {
        albumIds.forEach { id ->
            beachAlbumLocalDataSource.deleteBeachAlbum(familyId, id)
            beachMediaLocalDataSource.deleteBeachMediaByAlbumId(familyId, id)
            beachFirestoreDataSource.deleteBeachAlbum(id)
        }
        return Result.Success(Unit)
    }

    override suspend fun uploadBeachMedia(
        familyId: String,
        albumId: String,
        imageUris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Result<Unit, AppError> {
        return appResultOfSuspend {
            val total = imageUris.size
            val now = Date()
            val createdMediaList = mutableListOf<BeachMedia>()
            val createdEntities = mutableListOf<BeachMediaEntity>()

            imageUris.forEachIndexed { index, uri ->
                onProgress(index + 1, total)
                val imageType = ImageType.BeachMedia(beachAlbumId = albumId)
                when (val uploadResult = storageDataSource.uploadPhoto(uri, imageType, context)) {
                    is Result.Success -> {
                        val mediaId = UUID.randomUUID().toString()
                        val beachMedia = BeachMedia(
                            id = mediaId,
                            familyId = familyId,
                            beachAlbumId = albumId,
                            url = uploadResult.data.downloadUrl,
                            storagePath = "beaches/$familyId/$albumId/$mediaId",
                            thumbnail = uploadResult.data.byteArray,
                            createdAt = now,
                            lastUpdated = now,
                        )
                        val entity = BeachMediaEntity(
                            id = beachMedia.id,
                            familyId = beachMedia.familyId,
                            beachAlbumId = beachMedia.beachAlbumId,
                            url = beachMedia.url,
                            storagePath = beachMedia.storagePath,
                            thumbnail = beachMedia.thumbnail,
                            createdAt = beachMedia.createdAt,
                            lastUpdated = beachMedia.lastUpdated,
                        )
                        createdMediaList.add(beachMedia)
                        createdEntities.add(entity)
                    }
                    is Result.Failure -> throw AppErrorThrowable(uploadResult.error)
                }
            }

            // Local optimistic insert
            beachMediaLocalDataSource.upsertBeachMedia(createdEntities)

            // Update album count
            val albumEntity = beachAlbumLocalDataSource.getBeachAlbumOnce(familyId, albumId)
            if (albumEntity != null) {
                val updatedAlbum = albumEntity.toDomain().copy(
                    count = albumEntity.count + createdMediaList.size,
                    lastUpdated = now,
                )
                beachAlbumLocalDataSource.upsertBeachAlbum(
                    BeachAlbumEntity(
                        id = updatedAlbum.id,
                        familyId = updatedAlbum.familyId,
                        name = updatedAlbum.name,
                        count = updatedAlbum.count,
                        createdAt = updatedAlbum.createdAt,
                        lastUpdated = updatedAlbum.lastUpdated,
                    )
                )
                beachFirestoreDataSource.upsertBeachAlbum(familyId, updatedAlbum)
            }

            // Firestore write
            val remoteResult = beachFirestoreDataSource.upsertBeachMedia(familyId, createdMediaList)
            if (remoteResult is Result.Failure) {
                throw AppErrorThrowable(remoteResult.error)
            }
        }
    }

    override suspend fun deleteBeachMedia(familyId: String, mediaIds: List<String>, albumId: String): Result<Unit, AppError> {
        val now = Date()
        beachMediaLocalDataSource.deleteBeachMedia(familyId, mediaIds)

        val albumEntity = beachAlbumLocalDataSource.getBeachAlbumOnce(familyId, albumId)
        if (albumEntity != null) {
            val newCount = (albumEntity.count - mediaIds.size).coerceAtLeast(0)
            val updatedAlbum = albumEntity.toDomain().copy(count = newCount, lastUpdated = now)
            beachAlbumLocalDataSource.upsertBeachAlbum(
                BeachAlbumEntity(
                    id = updatedAlbum.id,
                    familyId = updatedAlbum.familyId,
                    name = updatedAlbum.name,
                    count = updatedAlbum.count,
                    createdAt = updatedAlbum.createdAt,
                    lastUpdated = updatedAlbum.lastUpdated,
                )
            )
            beachFirestoreDataSource.upsertBeachAlbum(familyId, updatedAlbum)
        }

        return beachFirestoreDataSource.deleteBeachMedia(mediaIds)
    }
}
