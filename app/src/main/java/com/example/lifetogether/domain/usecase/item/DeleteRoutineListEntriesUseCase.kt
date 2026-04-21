package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.logic.AppErrors

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
        if (entries.isEmpty()) return Result.Success(Unit)

        return try {
            val imageUrls = entries.mapNotNull { it.imageUrl }
            val imageDeleteResult = imageRepository.deleteMediaFiles(imageUrls)
            if (imageDeleteResult is Result.Failure) return imageDeleteResult

            val entryIds = entries.mapNotNull { it.id }
            if (entryIds.isEmpty()) return Result.Success(Unit)

            val remoteDeleteResult = userListRepository.deleteRoutineListEntries(entryIds)
            if (remoteDeleteResult is Result.Failure) return remoteDeleteResult
            remoteDeleteResult
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }
}
