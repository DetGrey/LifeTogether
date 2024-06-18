package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.Item
import javax.inject.Inject
import kotlin.reflect.KClass

class FetchListItemsUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val localListRepository: LocalListRepositoryImpl,
) {
    suspend operator fun <T : Item> invoke(
        uid: String,
        listName: String,
        itemType: KClass<T>,
    ): ListItemsResultListener<T> {
        println("Inside FetchListItemsUseCase and trying to fetch from local storage")
        return localListRepository.fetchListItems(listName, uid, itemType)
//        return remoteListRepository.fetchListItems(listName, uid, itemType)
    }
}
