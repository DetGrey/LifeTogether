package com.example.lifetogether.ui.feature.lists.entryDetails

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import java.util.Date
import javax.inject.Inject

class ListEntryDetailsSaver @Inject constructor(
    private val userListRepository: UserListRepository,
    private val uploadImageUseCase: UploadImageUseCase,
) {
    suspend fun save(
        details: EntryDetailsContent,
        entryId: String?,
        familyId: String,
        listId: String,
        now: Date,
        context: Context,
    ): Result<Unit, AppError> {
        validate(details)?.let { return Result.Failure(AppError.Validation(it)) }

        return when (details) {
            is EntryDetailsContent.Routine -> saveRoutine(details, entryId, familyId, listId, now, context)
            is EntryDetailsContent.Wish -> saveWish(details, entryId, familyId, listId, now)
            is EntryDetailsContent.Note -> saveNote(details, entryId, familyId, listId, now)
            is EntryDetailsContent.Checklist -> saveChecklist(details, entryId, familyId, listId, now)
            is EntryDetailsContent.Meal -> saveMeal(details, entryId, familyId, listId, now)
        }
    }

    suspend fun uploadRoutineImage(
        uri: Uri,
        familyId: String,
        entryId: String,
        context: Context,
    ): Result<Unit, AppError> {
        return uploadImageUseCase.invoke(
            uri = uri,
            imageType = ImageType.RoutineListEntryImage(familyId, entryId),
            context = context,
        )
    }

    private suspend fun saveRoutine(
        details: EntryDetailsContent.Routine,
        entryId: String?,
        familyId: String,
        listId: String,
        now: Date,
        context: Context,
    ): Result<Unit, AppError> {
        val form = details.form
        val interval = form.interval.toInt()
        val weekdays = form.selectedWeekdays.sorted()
        val tempEntry = RoutineListEntry(
            id = entryId ?: "",
            familyId = familyId,
            listId = listId,
            itemName = form.name.trim(),
            lastUpdated = now,
            dateCreated = now,
            nextDate = now,
            recurrenceUnit = form.recurrenceUnit,
            interval = interval,
            weekdays = weekdays,
        )
        val calculatedNextDate = RecurrenceCalculator.nextDate(tempEntry, now)
        val draft = tempEntry.copy(nextDate = calculatedNextDate)

        return if (entryId == null) {
            when (val saveResult = userListRepository.saveRoutineListEntry(draft)) {
                is Result.Success -> {
                    form.pendingImageUri?.let { pendingUri ->
                        uploadImageUseCase.invoke(
                            uri = pendingUri,
                            imageType = ImageType.RoutineListEntryImage(familyId, saveResult.data),
                            context = context,
                        )
                    }
                    Result.Success(Unit)
                }

                is Result.Failure -> Result.Failure(saveResult.error)
            }
        } else {
            when (val updateResult = userListRepository.updateRoutineListEntry(draft)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Failure -> Result.Failure(updateResult.error)
            }
        }
    }

    private suspend fun saveWish(
        details: EntryDetailsContent.Wish,
        entryId: String?,
        familyId: String,
        listId: String,
        now: Date,
    ): Result<Unit, AppError> {
        val form = details.form
        val draft = WishListEntry(
            id = entryId ?: "",
            familyId = familyId,
            listId = listId,
            itemName = form.name.trim(),
            lastUpdated = now,
            dateCreated = now,
            isPurchased = form.isPurchased,
            url = form.url.ifBlank { null },
            estimatedPriceMinor = form.estimatedPriceMinor.toLongOrNull(),
            currencyCode = form.currencyCode.ifBlank { null },
            priority = form.priority,
            notes = form.notes.ifBlank { null },
        )
        return if (entryId == null) {
            userListRepository.saveWishListEntry(draft).asUnit()
        } else {
            userListRepository.updateWishListEntry(draft).asUnit()
        }
    }

    private suspend fun saveNote(
        details: EntryDetailsContent.Note,
        entryId: String?,
        familyId: String,
        listId: String,
        now: Date,
    ): Result<Unit, AppError> {
        val form = details.form
        val draft = NoteEntry(
            id = entryId ?: "",
            familyId = familyId,
            listId = listId,
            itemName = form.name.trim(),
            markdownBody = form.markdownBody,
            lastUpdated = now,
            dateCreated = now,
        )
        return if (entryId == null) {
            userListRepository.saveNoteEntry(draft).asUnit()
        } else {
            userListRepository.updateNoteEntry(draft).asUnit()
        }
    }

    private suspend fun saveChecklist(
        details: EntryDetailsContent.Checklist,
        entryId: String?,
        familyId: String,
        listId: String,
        now: Date,
    ): Result<Unit, AppError> {
        val form = details.form
        val draft = ChecklistEntry(
            id = entryId ?: "",
            familyId = familyId,
            listId = listId,
            itemName = form.name.trim(),
            isChecked = form.isChecked,
            lastUpdated = now,
            dateCreated = now,
        )
        return if (entryId == null) {
            userListRepository.saveChecklistEntry(draft).asUnit()
        } else {
            userListRepository.updateChecklistEntry(draft).asUnit()
        }
    }

    private suspend fun saveMeal(
        details: EntryDetailsContent.Meal,
        entryId: String?,
        familyId: String,
        listId: String,
        now: Date,
    ): Result<Unit, AppError> {
        val form = details.form
        val draft = MealPlanEntry(
            id = entryId ?: "",
            familyId = familyId,
            listId = listId,
            itemName = form.name.trim(),
            date = form.date,
            recipeId = form.recipeId.ifBlank { null },
            customMealName = form.customMealName.ifBlank { null },
            lastUpdated = now,
            dateCreated = now,
        )
        return if (entryId == null) {
            userListRepository.saveMealPlanEntry(draft).asUnit()
        } else {
            userListRepository.updateMealPlanEntry(draft).asUnit()
        }
    }

    private fun validate(details: EntryDetailsContent): String? {
        return when (details) {
            is EntryDetailsContent.Routine -> {
                if (details.form.name.isBlank()) return "Name cannot be empty"
                val intervalInt = details.form.interval.toIntOrNull()
                if (intervalInt == null || intervalInt < 1) return "Interval must be at least 1"
                null
            }

            is EntryDetailsContent.Wish -> {
                if (details.form.name.isBlank()) return "Name cannot be empty"
                if (details.form.estimatedPriceMinor.isNotBlank() && details.form.estimatedPriceMinor.toLongOrNull() == null) {
                    return "Estimated price must be a whole number"
                }
                null
            }

            is EntryDetailsContent.Note -> if (details.form.name.isBlank()) "Name cannot be empty" else null
            is EntryDetailsContent.Checklist -> if (details.form.name.isBlank()) "Name cannot be empty" else null
            is EntryDetailsContent.Meal -> {
                if (details.form.name.isBlank()) return "Name cannot be empty"
                if (details.form.date.isBlank()) return "Date cannot be empty"
                val hasRecipeId = details.form.recipeId.isNotBlank()
                val hasCustomMeal = details.form.customMealName.isNotBlank()
                if (hasRecipeId == hasCustomMeal) {
                    return "Set either recipe ID or custom meal name"
                }
                null
            }
        }
    }

    private fun <T> Result<T, AppError>.asUnit(): Result<Unit, AppError> {
        return when (this) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(error)
        }
    }
}
