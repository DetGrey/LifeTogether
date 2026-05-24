package com.example.lifetogether.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.lifetogether.data.local.MealNotificationPrefsKeys
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.notification.MealNotificationPreferences
import com.example.lifetogether.domain.repository.MealNotificationPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealNotificationPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : MealNotificationPreferencesRepository {

    override fun observePreferences(): Flow<MealNotificationPreferences> =
        dataStore.data.map { prefs ->
            val default = MealNotificationPreferences()
            MealNotificationPreferences(
                masterEnabled = prefs[MealNotificationPrefsKeys.MASTER_ENABLED] ?: default.masterEnabled,
                onboardingShown = prefs[MealNotificationPrefsKeys.ONBOARDING_SHOWN] ?: default.onboardingShown,
                breakfastEnabled = prefs[MealNotificationPrefsKeys.BREAKFAST_ENABLED] ?: default.breakfastEnabled,
                lunchEnabled = prefs[MealNotificationPrefsKeys.LUNCH_ENABLED] ?: default.lunchEnabled,
                dinnerEnabled = prefs[MealNotificationPrefsKeys.DINNER_ENABLED] ?: default.dinnerEnabled,
                snackEnabled = prefs[MealNotificationPrefsKeys.SNACK_ENABLED] ?: default.snackEnabled,
                breakfastHour = prefs[MealNotificationPrefsKeys.BREAKFAST_HOUR] ?: default.breakfastHour,
                breakfastMinute = prefs[MealNotificationPrefsKeys.BREAKFAST_MINUTE] ?: default.breakfastMinute,
                lunchHour = prefs[MealNotificationPrefsKeys.LUNCH_HOUR] ?: default.lunchHour,
                lunchMinute = prefs[MealNotificationPrefsKeys.LUNCH_MINUTE] ?: default.lunchMinute,
                dinnerHour = prefs[MealNotificationPrefsKeys.DINNER_HOUR] ?: default.dinnerHour,
                dinnerMinute = prefs[MealNotificationPrefsKeys.DINNER_MINUTE] ?: default.dinnerMinute,
                snackHour = prefs[MealNotificationPrefsKeys.SNACK_HOUR] ?: default.snackHour,
                snackMinute = prefs[MealNotificationPrefsKeys.SNACK_MINUTE] ?: default.snackMinute,
            )
        }

    override suspend fun updateMasterEnabled(enabled: Boolean) {
        dataStore.edit { it[MealNotificationPrefsKeys.MASTER_ENABLED] = enabled }
    }

    override suspend fun updateOnboardingShown() {
        dataStore.edit { it[MealNotificationPrefsKeys.ONBOARDING_SHOWN] = true }
    }

    override suspend fun updateMealTypeEnabled(mealType: MealType, enabled: Boolean) {
        dataStore.edit { prefs ->
            when (mealType) {
                MealType.BREAKFAST -> prefs[MealNotificationPrefsKeys.BREAKFAST_ENABLED] = enabled
                MealType.LUNCH -> prefs[MealNotificationPrefsKeys.LUNCH_ENABLED] = enabled
                MealType.DINNER -> prefs[MealNotificationPrefsKeys.DINNER_ENABLED] = enabled
                MealType.SNACK -> prefs[MealNotificationPrefsKeys.SNACK_ENABLED] = enabled
                MealType.OTHER -> Unit
            }
        }
    }

    override suspend fun updateMealTypeTime(mealType: MealType, hour: Int, minute: Int) {
        dataStore.edit { prefs ->
            when (mealType) {
                MealType.BREAKFAST -> {
                    prefs[MealNotificationPrefsKeys.BREAKFAST_HOUR] = hour
                    prefs[MealNotificationPrefsKeys.BREAKFAST_MINUTE] = minute
                }
                MealType.LUNCH -> {
                    prefs[MealNotificationPrefsKeys.LUNCH_HOUR] = hour
                    prefs[MealNotificationPrefsKeys.LUNCH_MINUTE] = minute
                }
                MealType.DINNER -> {
                    prefs[MealNotificationPrefsKeys.DINNER_HOUR] = hour
                    prefs[MealNotificationPrefsKeys.DINNER_MINUTE] = minute
                }
                MealType.SNACK -> {
                    prefs[MealNotificationPrefsKeys.SNACK_HOUR] = hour
                    prefs[MealNotificationPrefsKeys.SNACK_MINUTE] = minute
                }
                MealType.OTHER -> Unit
            }
        }
    }
}
