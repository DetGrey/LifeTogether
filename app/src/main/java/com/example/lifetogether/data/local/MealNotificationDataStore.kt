package com.example.lifetogether.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.mealNotificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "meal_notifications",
)

object MealNotificationPrefsKeys {
    val MASTER_ENABLED = booleanPreferencesKey("master_enabled")
    val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
    val BREAKFAST_ENABLED = booleanPreferencesKey("breakfast_enabled")
    val LUNCH_ENABLED = booleanPreferencesKey("lunch_enabled")
    val DINNER_ENABLED = booleanPreferencesKey("dinner_enabled")
    val SNACK_ENABLED = booleanPreferencesKey("snack_enabled")
    val BREAKFAST_HOUR = intPreferencesKey("breakfast_hour")
    val BREAKFAST_MINUTE = intPreferencesKey("breakfast_minute")
    val LUNCH_HOUR = intPreferencesKey("lunch_hour")
    val LUNCH_MINUTE = intPreferencesKey("lunch_minute")
    val DINNER_HOUR = intPreferencesKey("dinner_hour")
    val DINNER_MINUTE = intPreferencesKey("dinner_minute")
    val SNACK_HOUR = intPreferencesKey("snack_hour")
    val SNACK_MINUTE = intPreferencesKey("snack_minute")
}
