package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        itemId: String,
        listName: String,
    ): ResultListener {
        return remoteListRepository.deleteItem(itemId, listName)
    }
}
