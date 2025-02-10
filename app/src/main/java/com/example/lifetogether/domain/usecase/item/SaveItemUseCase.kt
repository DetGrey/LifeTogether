package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.Item
import javax.inject.Inject

class SaveItemUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        item: Item,
        listName: String,
    ): StringResultListener {
        return remoteListRepository.saveItem(item, listName)
    }
}
