package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface MealPlannerRepository {
    fun observeMealPlans(familyId: String): Flow<Result<List<MealPlan>, AppError>>
    fun syncMealPlansFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    fun observeMealPlan(mealPlanId: String): Flow<Result<MealPlan, AppError>>
    suspend fun saveMealPlan(mealPlan: MealPlan): Result<String, AppError>
    suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit, AppError>
    suspend fun deleteMealPlans(mealPlanIds: List<String>): Result<Unit, AppError>
}
