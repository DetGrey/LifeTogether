package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.local.source.MealPlanLocalDataSource
import com.example.lifetogether.data.model.MealPlanEntity
import com.example.lifetogether.data.remote.MealPlanFirestoreDataSource
import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.repository.MealPlannerRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MealPlannerRepositoryImpl @Inject constructor(
    private val mealPlanFirestoreDataSource: MealPlanFirestoreDataSource,
    private val mealPlanLocalDataSource: MealPlanLocalDataSource,
) : MealPlannerRepository {
    override fun observeMealPlans(familyId: String): Flow<Result<List<MealPlan>, AppError>> {
        return mealPlanLocalDataSource.observeMealPlansByFamilyId(familyId)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedByDescending { it.date }
                }
            }
    }

    override fun syncMealPlansFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return mealPlanFirestoreDataSource.familyMealPlansSnapshotListener(familyId)
            .map { result ->
                when (result) {
                    is Result.Success -> appResultOfSuspend {
                        val visibleEntries = result.data.items
                        if (visibleEntries.isEmpty()) {
                            mealPlanLocalDataSource.deleteFamilyMealPlans(familyId)
                        } else {
                            mealPlanLocalDataSource.updateMealPlans(visibleEntries)
                        }
                    }

                    is Result.Failure -> Result.Failure(result.error)
                }
            }
    }

    override fun observeMealPlan(mealPlanId: String): Flow<Result<MealPlan, AppError>> {
        return mealPlanLocalDataSource.observeMealPlan(mealPlanId)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveMealPlan(mealPlan: MealPlan): Result<String, AppError> {
        return mealPlanFirestoreDataSource.saveMealPlan(mealPlan)
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit, AppError> {
        return mealPlanFirestoreDataSource.updateMealPlan(mealPlan)
    }

    override suspend fun deleteMealPlans(mealPlanIds: List<String>): Result<Unit, AppError> {
        return mealPlanFirestoreDataSource.deleteMealPlans(mealPlanIds)
    }

    private fun MealPlanEntity.toModel() = MealPlan(
        id = id,
        familyId = familyId,
        itemName = itemName,
        date = date,
        recipeId = recipeId,
        customMealName = customMealName,
        mealType = com.example.lifetogether.domain.model.lists.MealType.fromValue(mealType)
            ?: com.example.lifetogether.domain.model.lists.MealType.DINNER,
        notes = notes,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )
}
