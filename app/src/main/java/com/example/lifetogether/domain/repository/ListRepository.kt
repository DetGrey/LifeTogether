package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item

interface ListRepository {
    suspend fun saveItem(item: Item, listName: String): ResultListener
//    suspend fun toggleItemCompletion(item: Item, listName: String): ResultListener
//    suspend fun <T : Item> fetchListItems(listName: String, familyId: String, itemType: KClass<T>): Flow<ListItemsResultListener<T>>
}
