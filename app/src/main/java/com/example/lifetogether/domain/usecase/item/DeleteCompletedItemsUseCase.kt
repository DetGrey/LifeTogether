package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import javax.inject.Inject

class DeleteCompletedItemsUseCase @Inject constructor(
    private val remoteListRepositoryImpl: RemoteListRepositoryImpl,
    private val localListRepositoryImpl: LocalListRepositoryImpl,
) {
    suspend operator fun invoke(
        listName: String,
        items: List<Item>,
    ): ResultListener {
        println("Inside DeleteCompletedItemsUseCase and trying to delete items")
        val firestoreResult = remoteListRepositoryImpl.deleteItems(listName, items)
        val roomResult = localListRepositoryImpl.deleteItems(listName, items.map { it.id.toString() })
        return if (firestoreResult == ResultListener.Success) {
            roomResult
        } else {
            firestoreResult
        }
    }
}
