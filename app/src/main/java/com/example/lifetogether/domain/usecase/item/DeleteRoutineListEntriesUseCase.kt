package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteImageRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.util.Constants
import javax.inject.Inject

class DeleteRoutineListEntriesUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val localListRepository: LocalListRepositoryImpl,
    private val remoteImageRepository: RemoteImageRepositoryImpl,
) {
    suspend operator fun invoke(
        entries: List<RoutineListEntry>,
    ): ResultListener {
        if (entries.isEmpty()) return ResultListener.Success

        return try {
            val imageUrls = entries.mapNotNull { it.imageUrl }
            val imageDeleteResult = remoteImageRepository.deleteMediaFiles(imageUrls)
            if (imageDeleteResult !is ResultListener.Success) {
                return imageDeleteResult
            }

            val entryIds = entries.mapNotNull { it.id }
            if (entryIds.isEmpty()) return ResultListener.Success

            val remoteDeleteResult = remoteListRepository.deleteItems(
                Constants.ROUTINE_LIST_ENTRIES_TABLE,
                entryIds,
            )
            if (remoteDeleteResult !is ResultListener.Success) {
                return remoteDeleteResult
            }

            localListRepository.deleteItems(Constants.ROUTINE_LIST_ENTRIES_TABLE, entryIds)
        } catch (e: Exception) {
            ResultListener.Failure(e.message ?: "Unknown error occurred")
        }
    }
}
