package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.BeachAlbumsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.BeachAlbumEntity
import com.example.lifetogether.domain.model.beach.BeachAlbum
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BeachAlbumLocalDataSource @Inject constructor(
    private val beachAlbumsDao: BeachAlbumsDao,
) {
    fun observeBeachAlbums(familyId: String): Flow<List<BeachAlbumEntity>> =
        beachAlbumsDao.getItems(familyId)

    fun observeBeachAlbumById(familyId: String, albumId: String): Flow<BeachAlbumEntity?> =
        beachAlbumsDao.getItemByIdFlow(familyId, albumId)

    suspend fun updateBeachAlbums(familyId: String, items: List<BeachAlbum>) {
        val entities = items.map { item ->
            BeachAlbumEntity(
                id = item.id,
                familyId = familyId,
                name = item.name,
                count = item.count,
                createdAt = item.createdAt,
                lastUpdated = item.lastUpdated,
            )
        }
        val currentItems = beachAlbumsDao.getItems(familyId).first()
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
        beachAlbumsDao.updateItems(itemsToUpdate)
        beachAlbumsDao.deleteItems(familyId, itemsToDelete.map { it.id })
    }

    suspend fun getBeachAlbumOnce(familyId: String, id: String): BeachAlbumEntity? =
        beachAlbumsDao.getItemOnce(familyId, id)

    suspend fun upsertBeachAlbum(entity: BeachAlbumEntity) =
        beachAlbumsDao.updateItems(listOf(entity))

    suspend fun deleteBeachAlbum(familyId: String, id: String) =
        beachAlbumsDao.deleteItems(familyId, listOf(id))
}
