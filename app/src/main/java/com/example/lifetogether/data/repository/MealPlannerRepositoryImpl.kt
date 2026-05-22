package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.local.source.MealPlanLocalDataSource
import com.example.lifetogether.data.model.MealPlanEntity
import com.example.lifetogether.data.remote.MealPlanFirestoreDataSource
import com.example.lifetogether.data.repository.internal.stampNow
import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.repository.MealPlannerRepository
import com.example.lifetogether.data.logic.AppErrors
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
                            mealPlanLocalDataSource.updateMealPlans(visibleEntries.map { it.toEntity() })
                        }
                    }

                    is Result.Failure -> Result.Failure(result.error)
                }
            }
    }

    override fun observeMealPlan(mealPlanId: String): Flow<Result<MealPlan, AppError>> {
        return mealPlanLocalDataSource.observeMealPlan(mealPlanId).map { entity ->
            if (entity == null) return@map Result.Failure(AppErrors.notFound("Meal plan not found"))
            appResultOf { entity.toModel() }
        }
    }

    override suspend fun saveMealPlan(mealPlan: MealPlan): Result<String, AppError> {
        val stampedMealPlan = mealPlan.stampNow()
        mealPlanLocalDataSource.upsertMealPlan(stampedMealPlan.toEntity())
        return when (val result = mealPlanFirestoreDataSource.saveMealPlan(stampedMealPlan)) {
            is Result.Success -> Result.Success(stampedMealPlan.id)
            is Result.Failure -> {
                mealPlanLocalDataSource.deleteMealPlan(stampedMealPlan.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit, AppError> {
        val stampedMealPlan = mealPlan.stampNow()
        val oldEntity = mealPlanLocalDataSource.getMealPlanOnce(mealPlan.id)
        mealPlanLocalDataSource.upsertMealPlan(stampedMealPlan.toEntity())
        return when (val result = mealPlanFirestoreDataSource.updateMealPlan(stampedMealPlan)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) mealPlanLocalDataSource.upsertMealPlan(oldEntity)
                else mealPlanLocalDataSource.deleteMealPlan(stampedMealPlan.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteMealPlan(mealPlanId: String): Result<Unit, AppError> {
        val oldEntity = mealPlanLocalDataSource.getMealPlanOnce(mealPlanId)
        mealPlanLocalDataSource.deleteMealPlan(mealPlanId)
        return when (val result = mealPlanFirestoreDataSource.deleteMealPlan(mealPlanId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) mealPlanLocalDataSource.upsertMealPlan(oldEntity)
                Result.Failure(result.error)
            }
        }
    }

    private fun MealPlan.toEntity() = MealPlanEntity(
        id = id,
        familyId = familyId,
        itemName = itemName,
        date = date,
        recipeId = recipeId,
        customMealName = customMealName,
        mealType = mealType.name,
        notes = notes,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )

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
