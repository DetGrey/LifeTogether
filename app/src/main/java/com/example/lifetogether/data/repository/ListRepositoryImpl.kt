package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import kotlin.reflect.KClass

class ListRepositoryImpl : ListRepository {
    private val firestoreDataSource = FirestoreDataSource()

    override suspend fun saveItem(
        item: Item,
        listName: String,
    ): ResultListener {
        return firestoreDataSource.saveItem(item, listName)
    }

    override suspend fun toggleItemCompletion(
        item: Item,
        listName: String,
    ): ResultListener {
        return firestoreDataSource.toggleItemCompletion(item, listName)
    }

    override suspend fun fetchListDefaults(listName: String): DefaultsResultListener {
        return firestoreDataSource.fetchListDefaults(listName)
    }

    override suspend fun <T : Item> fetchListItems(
        listName: String,
        uid: String,
        itemType: KClass<T>,
    ): ListItemsResultListener<T> {
        return firestoreDataSource.fetchListItems(listName, uid, itemType)
    }
}
