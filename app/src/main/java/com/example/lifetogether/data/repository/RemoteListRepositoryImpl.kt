package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import javax.inject.Inject

class RemoteListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
) : ListRepository {
    // ------------------------------------------ GENERAL LISTS
    override suspend fun saveItem(
        item: Item,
        listName: String,
    ): StringResultListener {
        return firestoreDataSource.saveItem(item, listName)
    }

    suspend fun updateItem(
        item: Item,
        listName: String,
    ): ResultListener {
        return firestoreDataSource.updateItem(item, listName)
    }

    suspend fun toggleCompletableItemCompletion(
        item: CompletableItem,
        listName: String,
    ): ResultListener {
        return firestoreDataSource.toggleCompletableItemCompletion(item, listName)
    }

    suspend fun deleteItem(
        itemId: String,
        listName: String,
    ): ResultListener {
        return firestoreDataSource.deleteItem(itemId, listName)
    }

    suspend fun deleteItems(
        listName: String,
        idsList: List<String>,
    ): ResultListener {
        return firestoreDataSource.deleteItems(listName, idsList)
    }
    // ------------------------------------------ CUSTOM FUNCTIONS
    suspend fun updateAlbumCount(
        albumId: String,
        count: Int,
    ): ResultListener {
        return firestoreDataSource.updateAlbumCount(albumId, count)
    }
    suspend fun moveMediaToAlbum(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): ResultListener {
        return firestoreDataSource.moveMediaToAlbum(mediaIdList, newAlbumId, oldAlbumId)
    }
}
