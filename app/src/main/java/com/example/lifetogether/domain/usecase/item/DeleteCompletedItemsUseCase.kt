package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class DeleteCompletedItemsUseCase @Inject constructor(
    private val remoteListRepositoryImpl: RemoteListRepositoryImpl,
    private val localListRepositoryImpl: LocalListRepositoryImpl,
) {
    suspend operator fun invoke(
        listName: String,
        idsList: List<String>,
    ): ResultListener {
        println("Inside DeleteCompletedItemsUseCase and trying to delete items")
        val firestoreResult = remoteListRepositoryImpl.deleteItems(listName, idsList)
        val roomResult = localListRepositoryImpl.deleteItems(listName, idsList)
        return if (firestoreResult == ResultListener.Success) {
            roomResult
        } else {
            firestoreResult
        }
    }
}
