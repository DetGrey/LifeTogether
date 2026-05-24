package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.repository.MealPlannerRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class MealPlanDetailsSaver @Inject constructor(
    private val mealPlannerRepository: MealPlannerRepository,
) {
    suspend fun save(
        form: MealPlanFormState,
        mealRecipeSearchState: MealRecipeSearchState,
        mealPlanId: String?,
        familyId: String,
        now: Date,
    ): Result<Unit, AppError> {
        validate(form, mealRecipeSearchState)?.let { return Result.Failure(AppError.Validation(it)) }
        return saveMeal(form, mealRecipeSearchState, mealPlanId, familyId, now)
    }

    suspend fun deleteMealPlan(mealPlanId: String): Result<Unit, AppError> {
        return mealPlannerRepository.deleteMealPlan(mealPlanId)
    }

    private suspend fun saveMeal(
        form: MealPlanFormState,
        mealRecipeSearchState: MealRecipeSearchState,
        mealPlanId: String?,
        familyId: String,
        now: Date,
    ): Result<Unit, AppError> {
        val mode = mealRecipeSearchState.mode
        val recipeSearchItem = mealRecipeSearchState.selectedRecipeSearchItem
        val customMealName = form.customMealName?.trim()
        val recipeId = when (mode) {
            MealSearchMode.RECIPE -> recipeSearchItem?.id
            MealSearchMode.CUSTOM -> null
        }
        val itemName = when (mode) {
            MealSearchMode.RECIPE -> recipeSearchItem?.itemName.orEmpty()
            MealSearchMode.CUSTOM -> customMealName.orEmpty()
        }
        val draft = MealPlan(
            id = mealPlanId ?: UUID.randomUUID().toString(),
            familyId = familyId,
            itemName = itemName,
            date = form.date,
            recipeId = recipeId,
            customMealName = customMealName?.takeIf { it.isNotBlank() },
            mealType = form.mealType,
            notes = form.notes,
            dateCreated = now,
        )
        return if (mealPlanId == null) {
            mealPlannerRepository.saveMealPlan(draft).asUnit()
        } else {
            mealPlannerRepository.updateMealPlan(draft).asUnit()
        }
    }

    private fun validate(
        form: MealPlanFormState,
        mealRecipeSearchState: MealRecipeSearchState?,
    ): String? {
        if (form.date.isBlank()) return "Date cannot be empty"
        val mode = mealRecipeSearchState?.mode ?: MealSearchMode.RECIPE
        return if (mode == MealSearchMode.RECIPE) {
            if (mealRecipeSearchState?.selectedRecipeSearchItem == null) "Select a recipe" else null
        } else {
            if (form.customMealName.isNullOrBlank()) "Custom meal name cannot be empty" else null
        }
    }

    private fun <T> Result<T, AppError>.asUnit(): Result<Unit, AppError> {
        return when (this) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(error)
        }
    }
}
