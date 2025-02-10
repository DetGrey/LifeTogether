package com.example.lifetogether.domain.callback

import com.example.lifetogether.domain.model.Item

sealed class ItemResultListener<out T : Item> {
    data class Success<out T : Item>(val item: T) : ItemResultListener<T>()
    data class Failure(val message: String) : ItemResultListener<Nothing>()
}
