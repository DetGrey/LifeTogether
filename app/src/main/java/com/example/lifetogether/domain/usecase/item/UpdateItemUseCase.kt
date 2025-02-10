package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        item: Item,
        listName: String,
    ): ResultListener {
        return remoteListRepository.updateItem(item, listName)
    }
}
