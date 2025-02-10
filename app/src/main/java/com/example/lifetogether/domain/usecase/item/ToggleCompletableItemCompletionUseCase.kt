package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.CompletableItem
import javax.inject.Inject

class ToggleCompletableItemCompletionUseCase @Inject constructor(
    private val listRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        item: CompletableItem,
        listName: String,
    ): ResultListener {
        return listRepository.toggleCompletableItemCompletion(item, listName)
    }
}
