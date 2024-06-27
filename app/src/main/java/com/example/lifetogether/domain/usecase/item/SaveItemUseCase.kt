package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import javax.inject.Inject

class SaveItemUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val localListRepository: LocalListRepositoryImpl,
) {
    suspend operator fun invoke(
        item: Item,
        listName: String,
    ): ResultListener {
        return remoteListRepository.saveItem(item, listName)
    }
}
