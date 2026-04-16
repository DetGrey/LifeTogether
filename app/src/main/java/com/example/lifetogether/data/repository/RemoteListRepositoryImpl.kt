package com.example.lifetogether.data.repository

import android.util.Log
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.LegacyListRepository
import com.example.lifetogether.domain.result.Result
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

    suspend fun updateItem(
        item: Item,
        listName: String,
    ): Result<Unit, String> {
        Log.d(TAG, "updateItem forwarding listName=$listName id=${item.id} type=${item::class.simpleName}")
        val result = firestoreDataSource.updateItem(item, listName)
        when (result) {
            is Result.Success -> Log.d(TAG, "updateItem success listName=$listName id=${item.id}")
            is Result.Failure -> Log.e(TAG, "updateItem failure listName=$listName id=${item.id} message=${result.error}")
        }
        return result
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
    ): Result<Unit, String> {
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
