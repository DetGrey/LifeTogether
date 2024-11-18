package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import javax.inject.Inject

class RemoteListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
) : ListRepository {
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

    suspend fun deleteItems(
        listName: String,
        items: List<Item>,
    ): ResultListener {
        return firestoreDataSource.deleteItems(listName, items)
    }
}
