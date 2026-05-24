package com.example.lifetogether.ui.feature.settings.notifications

import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.notification.MealNotificationPreferences

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState

    data class Content(
        val prefs: MealNotificationPreferences,
        val timePickerFor: MealType? = null,
        val hasExactAlarmPermission: Boolean = true,
    ) : NotificationsUiState
}

sealed interface NotificationsUiEvent {
    data object ToggleMaster : NotificationsUiEvent
    data object RefreshPermission : NotificationsUiEvent
    data class ToggleMealType(val mealType: MealType) : NotificationsUiEvent
    data class ShowTimePicker(val mealType: MealType) : NotificationsUiEvent
    data object DismissTimePicker : NotificationsUiEvent
    data class ConfirmTime(val mealType: MealType, val hour: Int, val minute: Int) : NotificationsUiEvent
    data object ResetTimes : NotificationsUiEvent
}

sealed interface NotificationsNavigationEvent {
    data object NavigateBack : NotificationsNavigationEvent
}
