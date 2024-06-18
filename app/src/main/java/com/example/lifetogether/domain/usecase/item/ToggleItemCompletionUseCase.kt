package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import javax.inject.Inject

class ToggleItemCompletionUseCase @Inject constructor(
    private val listRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        item: Item,
        listName: String,
    ): ResultListener {
        return listRepository.toggleItemCompletion(item, listName)
    }
}
