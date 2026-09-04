package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.BeachMediaDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.BeachMediaEntity
import com.example.lifetogether.domain.model.beach.BeachMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeachMediaLocalDataSource @Inject constructor(
    private val beachMediaDao: BeachMediaDao,
) {
    fun observeBeachMedia(familyId: String, albumId: String): Flow<List<BeachMediaEntity>> =
        beachMediaDao.getItemsByAlbumId(familyId, albumId)

    fun observeAlbumThumbnails(familyId: String): Flow<Map<String, ByteArray>> =
        beachMediaDao.observeAlbumThumbnails(familyId).map { list ->
            list.associate { it.albumId to it.thumbnail }
        }

    suspend fun updateBeachMedia(familyId: String, albumId: String, items: List<BeachMedia>) {
        val currentItems = beachMediaDao.getItemsByAlbumId(familyId, albumId).first()
        val existingMap = currentItems.associateBy { it.id }
        val entities = items.map { item ->
            val existing = existingMap[item.id]
            val resolvedThumbnail = item.thumbnail ?: existing?.thumbnail
            BeachMediaEntity(
                id = item.id,
                familyId = familyId,
                beachAlbumId = albumId,
                url = item.url,
                storagePath = item.storagePath,
                thumbnail = resolvedThumbnail,
                downloadState = if (resolvedThumbnail != null) com.example.lifetogether.domain.model.gallery.MediaDownloadState.READY else (existing?.downloadState ?: item.downloadState),
                lastDownloadAttempt = item.lastDownloadAttempt ?: existing?.lastDownloadAttempt,
                lastDownloadError = item.lastDownloadError ?: existing?.lastDownloadError,
                createdAt = item.createdAt,
                lastUpdated = item.lastUpdated,
            )
        }
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        val itemsToDelete = computeItemsToDelete(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        beachMediaDao.updateItems(itemsToUpdate)
        beachMediaDao.deleteItems(familyId, itemsToDelete.map { it.id })
    }

    suspend fun getItemsWithMissingThumbnails(familyId: String): List<BeachMediaEntity> =
        beachMediaDao.getItemsWithMissingThumbnails(familyId)

    suspend fun syncFamilyBeachMedia(familyId: String, items: List<BeachMedia>) {
        val currentItems = beachMediaDao.getItemsOnceByFamilyId(familyId)
        val existingMap = currentItems.associateBy { it.id }
        val entities = items.map { item ->
            val existing = existingMap[item.id]
            val resolvedThumbnail = item.thumbnail ?: existing?.thumbnail
            BeachMediaEntity(
                id = item.id,
                familyId = familyId,
                beachAlbumId = item.beachAlbumId,
                url = item.url,
                storagePath = item.storagePath,
                thumbnail = resolvedThumbnail,
                downloadState = if (resolvedThumbnail != null) com.example.lifetogether.domain.model.gallery.MediaDownloadState.READY else (existing?.downloadState ?: item.downloadState),
                lastDownloadAttempt = item.lastDownloadAttempt ?: existing?.lastDownloadAttempt,
                lastDownloadError = item.lastDownloadError ?: existing?.lastDownloadError,
                createdAt = item.createdAt,
                lastUpdated = item.lastUpdated,
            )
        }
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        val itemsToDelete = computeItemsToDelete(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        beachMediaDao.updateItems(itemsToUpdate)
        beachMediaDao.deleteItems(familyId, itemsToDelete.map { it.id })
    }

    suspend fun markMediaDownloadsPending(familyId: String, ids: List<String>) =
        beachMediaDao.markMediaDownloadsPending(familyId, ids)

    suspend fun markMediaDownloadFailure(id: String, error: String, attemptDate: java.util.Date) =
        beachMediaDao.markMediaDownloadFailure(id, error, attemptDate)

    suspend fun markMediaDownloadSuccess(id: String, thumbnail: ByteArray) =
        beachMediaDao.markMediaDownloadSuccess(id, thumbnail)

    suspend fun upsertBeachMedia(entities: List<BeachMediaEntity>) =
        beachMediaDao.updateItems(entities)

    suspend fun deleteBeachMedia(familyId: String, ids: List<String>) =
        beachMediaDao.deleteItems(familyId, ids)

    suspend fun deleteBeachMediaByAlbumId(familyId: String, albumId: String) =
        beachMediaDao.deleteItemsByAlbumId(familyId, albumId)
}

