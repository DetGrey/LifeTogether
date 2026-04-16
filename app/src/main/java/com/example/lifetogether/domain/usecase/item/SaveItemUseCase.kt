package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class SaveItemUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        item: Item,
        listName: String,
    ): StringResultListener {
        return when (val result = remoteListRepository.saveItem(item, listName)) {
            is Result.Success -> StringResultListener.Success(result.data)
            is Result.Failure -> StringResultListener.Failure(result.error)
        }
    }
}
