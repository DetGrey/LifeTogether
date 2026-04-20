package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.local.source.query.ListQueryType
import com.example.lifetogether.data.repository.ImageRepositoryImpl
import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import javax.inject.Inject

class DeleteRoutineListEntriesUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
    private val localListRepository: LocalListRepositoryImpl,
    private val imageRepositoryImpl: ImageRepositoryImpl,
) {
    suspend operator fun invoke(
        entries: List<RoutineListEntry>,
    ): Result<Unit, String> {
        if (entries.isEmpty()) return Result.Success(Unit)

        return try {
            val imageUrls = entries.mapNotNull { it.imageUrl }
            val imageDeleteResult = imageRepositoryImpl.deleteMediaFiles(imageUrls)
            if (imageDeleteResult is Result.Failure) return imageDeleteResult

            val entryIds = entries.mapNotNull { it.id }
            if (entryIds.isEmpty()) return Result.Success(Unit)

            val remoteDeleteResult = remoteListRepository.deleteItems(
                Constants.ROUTINE_LIST_ENTRIES_TABLE,
                entryIds,
            )
            if (remoteDeleteResult is Result.Failure) return remoteDeleteResult

            localListRepository.deleteItems(ListQueryType.RoutineListEntries, entryIds)
        } catch (e: Exception) {
            Result.Failure(e.message ?: "Unknown error occurred")
        }
    }
}
