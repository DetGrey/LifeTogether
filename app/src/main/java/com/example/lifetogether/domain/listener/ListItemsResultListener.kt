package com.example.lifetogether.domain.listener

import com.example.lifetogether.domain.model.Item

sealed class ListItemsResultListener<out T : Item> {
    data class Success<out T : Item>(
        val listItems: List<T>,
        val isFromCache: Boolean = false // Indicates if data is from local cache (offline) vs server
    ) : ListItemsResultListener<T>()
    data class Failure(val message: String) : ListItemsResultListener<Nothing>()
}
