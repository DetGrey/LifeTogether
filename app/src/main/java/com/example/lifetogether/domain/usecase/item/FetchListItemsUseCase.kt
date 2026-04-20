package com.example.lifetogether.domain.usecase.item

import android.util.Log
import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.listener.ListItemsResultListener
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
        uid: String? = null,
    ): Flow<ListItemsResultListener<Item>> {
        Log.d("FetchListItemsUseCase", "invoke")
        // Return a flow that emits updates to the list items
        return localListRepository.fetchListItems(listName, familyId, itemType, uid)
    }
}
