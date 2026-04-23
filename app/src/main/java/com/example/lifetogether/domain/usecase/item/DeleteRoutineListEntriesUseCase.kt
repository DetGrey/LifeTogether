package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class DeleteRoutineListEntriesUseCase @Inject constructor(
    private val userListRepository: UserListRepository,
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(
        entries: List<RoutineListEntry>,
    ): Result<Unit, AppError> {
        return appResultOfSuspend {
            if (entries.isEmpty()) return@appResultOfSuspend

            val imageUrls = entries.mapNotNull { it.imageUrl }
            val imageDeleteResult = imageRepository.deleteMediaFiles(imageUrls)
            if (imageDeleteResult is Result.Failure) throw AppErrorThrowable(imageDeleteResult.error)

            val entryIds = entries.mapNotNull { it.id }
            if (entryIds.isEmpty()) return@appResultOfSuspend

            val remoteDeleteResult = userListRepository.deleteRoutineListEntries(entryIds)
            if (remoteDeleteResult is Result.Failure) throw AppErrorThrowable(remoteDeleteResult.error)
        }
    }
}
