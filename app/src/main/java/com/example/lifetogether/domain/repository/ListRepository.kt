package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface ListRepository {
    suspend fun fetchAllData(uid: String): Flow<List<Any>>
    suspend fun saveItem(item: Item, listName: String): ResultListener
    suspend fun toggleItemCompletion(item: Item, listName: String): ResultListener
    suspend fun fetchListDefaults(listName: String): DefaultsResultListener
    suspend fun <T : Item> fetchListItems(listName: String, uid: String, itemType: KClass<T>): Flow<ListItemsResultListener<T>>
}
