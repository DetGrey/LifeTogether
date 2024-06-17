package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.Item
import kotlin.reflect.KClass

interface ListRepository {
    suspend fun saveItemToGroceryList(item: Item): ResultListener
    suspend fun toggleItemCompletionInGroceryList(item: GroceryItem): ResultListener
    suspend fun fetchListDefaults(listName: String): DefaultsResultListener
    suspend fun <T : Item> fetchListItems(listName: String, uid: String, itemType: KClass<T>): ListItemsResultListener<T>
}
