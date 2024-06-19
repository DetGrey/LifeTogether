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
        // Save to local storage first
        val localResult = localListRepository.saveItem(item, listName)
        if (localResult is ResultListener.Success) {
            // If successful, try saving to remote storage
            val remoteResult = remoteListRepository.saveItem(item, listName)
            if (remoteResult is ResultListener.Failure) {
                // TODO If remote save fails, mark item as needing sync
//                localListRepository.markItemAsNeedingSync(item)
            }
        }
        return localResult
    }
}
