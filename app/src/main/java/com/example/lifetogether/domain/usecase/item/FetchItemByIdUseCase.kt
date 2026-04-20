package com.example.lifetogether.domain.usecase.item

import android.util.Log
import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.listener.ItemResultListener
import com.example.lifetogether.domain.model.Item
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.reflect.KClass

class FetchItemByIdUseCase @Inject constructor(
    private val localListRepository: LocalListRepositoryImpl,
) {
    private companion object {
        const val TAG = "FetchItemByIdUseCase"
    }

    operator fun <T : Item> invoke(
        familyId: String,
        id: String,
        listName: String,
        itemType: KClass<T>,
        uid: String? = null,
    ): Flow<ItemResultListener<Item>> {
        Log.d(
            TAG,
            "invoke listName=$listName familyId=$familyId id=$id itemType=${itemType.simpleName}",
        )
        return localListRepository.fetchItemById(listName, familyId, id, itemType, uid)
    }
}
