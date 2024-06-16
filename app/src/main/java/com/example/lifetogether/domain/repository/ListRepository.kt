package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item

interface ListRepository {
    suspend fun saveItemToGroceryList(item: Item): ResultListener
}
