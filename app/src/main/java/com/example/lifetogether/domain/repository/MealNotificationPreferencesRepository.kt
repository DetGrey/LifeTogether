package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.notification.MealNotificationPreferences
import kotlinx.coroutines.flow.Flow

interface MealNotificationPreferencesRepository {
    fun observePreferences(): Flow<MealNotificationPreferences>
    suspend fun updateMasterEnabled(enabled: Boolean)
    suspend fun updateOnboardingShown()
    suspend fun updateMealTypeEnabled(mealType: MealType, enabled: Boolean)
    suspend fun updateMealTypeTime(mealType: MealType, hour: Int, minute: Int)
}
