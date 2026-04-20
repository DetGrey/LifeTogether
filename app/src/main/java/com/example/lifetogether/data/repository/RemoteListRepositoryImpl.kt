package com.example.lifetogether.data.repository

import android.util.Log
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.LegacyListRepository
import javax.inject.Inject

class RemoteListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
) : LegacyListRepository {
    private companion object {
        const val TAG = "RemoteListRepo"
    }

    // ------------------------------------------ GENERAL LISTS
    suspend fun saveItem(
        item: Item,
        listName: String,
    ): Result<String, String> {
        return firestoreDataSource.saveItem(item, listName)
    }

    suspend fun deleteItem(
        itemId: String,
        listName: String,
    ): Result<Unit, String> {
        return firestoreDataSource.deleteItem(itemId, listName)
    }

    suspend fun deleteItems(
        listName: String,
        idsList: List<String>,
    ): Result<Unit, String> {
        return firestoreDataSource.deleteItems(listName, idsList)
    }
    // ------------------------------------------ CUSTOM FUNCTIONS
    suspend fun updateAlbumCount(
        albumId: String,
        count: Int,
    ): Result<Unit, String> {
        return firestoreDataSource.updateAlbumCount(albumId, count)
    }
    suspend fun moveMediaToAlbum(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): Result<Unit, String> {
        return firestoreDataSource.moveMediaToAlbum(mediaIdList, newAlbumId, oldAlbumId)
    }
}
