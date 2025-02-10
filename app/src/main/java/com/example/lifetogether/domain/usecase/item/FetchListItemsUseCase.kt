package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.reflect.KClass

class FetchListItemsUseCase @Inject constructor(
    private val localListRepository: LocalListRepositoryImpl,
) {
    operator fun <T : Item> invoke(
        familyId: String,
        listName: String,
        itemType: KClass<T>,
    ): Flow<ListItemsResultListener<Item>> {
        println("Inside FetchListItemsUseCase and trying to fetch from local storage")
        // Return a flow that emits updates to the list items
        return localListRepository.fetchListItems(listName, familyId, itemType)
    }
}
