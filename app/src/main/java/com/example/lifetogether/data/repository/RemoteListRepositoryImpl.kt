package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import javax.inject.Inject

class RemoteListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
) : ListRepository {
    override suspend fun saveItem(
        item: Item,
        listName: String,
    ): ResultListener {
        return firestoreDataSource.saveItem(item, listName)
    }

    suspend fun toggleItemCompletion(
        item: Item,
        listName: String,
    ): ResultListener {
        return firestoreDataSource.toggleItemCompletion(item, listName)
    }
}
