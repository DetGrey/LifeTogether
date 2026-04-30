package com.example.lifetogether.domain.usecase.guide

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.domain.logic.GuideParser
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

data class ImportSummary(
    val successCount: Int,
    val failureCount: Int,
)

class ImportGuidesUseCase @Inject constructor(
    private val guideRepository: GuideRepository,
) {
    suspend operator fun invoke(
        json: String,
        familyId: String,
        ownerUid: String,
    ): Result<ImportSummary, AppError> {
        if (familyId.isBlank() || ownerUid.isBlank()) {
            return Result.Failure(AppError.Validation("Missing family or user context for import"))
        }
        
        if (json.isBlank()) {
            return Result.Failure(AppError.Validation("Import file is empty"))
        }

        val parsedGuides = try {
            GuideParser.parseJsonGuides(
                json = json,
                familyId = familyId,
                ownerUid = ownerUid,
            )
        } catch (e: Exception) {
            return Result.Failure(AppErrors.fromThrowable(e))
        }

        if (parsedGuides.isEmpty()) {
            return Result.Failure(AppError.Validation("No valid guides were found in the selected file"))
        }

        var successCount = 0
        var failureCount = 0
        
        parsedGuides.forEach { guide ->
            when (guideRepository.saveGuide(guide)) {
                is Result.Success -> successCount++
                is Result.Failure -> failureCount++
            }
        }

        return Result.Success(ImportSummary(successCount, failureCount))
    }
}