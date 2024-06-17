package com.example.lifetogether.domain.callback

import com.example.lifetogether.domain.model.Item

sealed class ListItemsResultListener<out T : Item> {
    data class Success<out T : Item>(val listItems: List<T>) : ListItemsResultListener<T>()
    data class Failure(val message: String) : ListItemsResultListener<Nothing>()
}
