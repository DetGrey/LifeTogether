package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.ListRepositoryImpl
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.Item
import kotlin.reflect.KClass

class FetchListItemsUseCase {
    private val listRepository = ListRepositoryImpl()
    suspend operator fun <T : Item> invoke(
        uid: String,
        listName: String,
        itemType: KClass<T>,
    ): ListItemsResultListener<T> {
        return listRepository.fetchListItems(listName, uid, itemType)
    }
}
