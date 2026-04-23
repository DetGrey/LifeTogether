package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.AlbumsDao
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.domain.model.gallery.Album
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumLocalDataSource @Inject constructor(
    private val albumsDao: AlbumsDao,
    private val galleryMediaDao: GalleryMediaDao,
) {
    fun observeAlbums(familyId: String): Flow<List<AlbumEntity>> = albumsDao.getItems(familyId)

    fun observeAlbumById(familyId: String, albumId: String): Flow<AlbumEntity?> =
        albumsDao.getItemByIdFlow(familyId, albumId)

    suspend fun updateAlbums(items: List<Album>) {
        val familyId = items.firstOrNull()?.familyId ?: return
        val entities = items.map { item ->
            AlbumEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                count = item.count,
            )
        }
        val currentItems = albumsDao.getItems(familyId).first()
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
        albumsDao.updateItems(itemsToUpdate)
        albumsDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyAlbums(familyId: String) {
        albumsDao.getItems(familyId).firstOrNull()?.let { currentFamilyItems ->
            albumsDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    fun getAlbumMedia(
        familyId: String,
        albumId: String,
    ): Flow<List<GalleryMediaEntity>> =
        galleryMediaDao.getItemsByAlbumId(familyId, albumId)
}
