package com.example.lifetogether.ui.feature.settings.notifications

import android.app.AlarmManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.notification.MealNotificationPreferences
import com.example.lifetogether.domain.model.notification.timeFor
import com.example.lifetogether.domain.model.notification.typeEnabledFor
import com.example.lifetogether.domain.notification.NOTIFIABLE_MEAL_TYPES
import com.example.lifetogether.domain.repository.MealNotificationPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val prefsRepository: MealNotificationPreferencesRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val _timePickerFor = MutableStateFlow<MealType?>(null)
    private val _hasExactAlarmPermission = MutableStateFlow(alarmManager.canScheduleExactAlarms())

    val uiState: StateFlow<NotificationsUiState> = combine(
        prefsRepository.observePreferences(),
        _timePickerFor,
        _hasExactAlarmPermission,
    ) { prefs, timePickerFor, hasPermission ->
        NotificationsUiState.Content(
            prefs = prefs,
            timePickerFor = timePickerFor,
            hasExactAlarmPermission = hasPermission,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState.Loading,
    )

    fun onUiEvent(event: NotificationsUiEvent) {
        when (event) {
            NotificationsUiEvent.ToggleMaster -> toggleMaster()
            NotificationsUiEvent.RefreshPermission -> refreshPermission()
            is NotificationsUiEvent.ToggleMealType -> toggleMealType(event.mealType)
            is NotificationsUiEvent.ShowTimePicker -> _timePickerFor.value = event.mealType
            NotificationsUiEvent.DismissTimePicker -> _timePickerFor.value = null
            is NotificationsUiEvent.ConfirmTime -> confirmTime(event.mealType, event.hour, event.minute)
            NotificationsUiEvent.ResetTimes -> resetTimes()
        }
    }

    private fun toggleMaster() {
        val content = uiState.value as? NotificationsUiState.Content ?: return
        if (!content.prefs.masterEnabled && !content.hasExactAlarmPermission) return
        viewModelScope.launch { prefsRepository.updateMasterEnabled(!content.prefs.masterEnabled) }
    }

    private fun refreshPermission() {
        val hasPermission = alarmManager.canScheduleExactAlarms()
        _hasExactAlarmPermission.value = hasPermission
        if (!hasPermission) {
            val masterEnabled = (uiState.value as? NotificationsUiState.Content)?.prefs?.masterEnabled ?: false
            if (masterEnabled) {
                viewModelScope.launch { prefsRepository.updateMasterEnabled(false) }
            }
        }
    }

    private fun toggleMealType(mealType: MealType) {
        val prefs = (uiState.value as? NotificationsUiState.Content)?.prefs ?: return
        viewModelScope.launch { prefsRepository.updateMealTypeEnabled(mealType, !prefs.typeEnabledFor(mealType)) }
    }

    private fun confirmTime(mealType: MealType, hour: Int, minute: Int) {
        _timePickerFor.value = null
        viewModelScope.launch { prefsRepository.updateMealTypeTime(mealType, hour, minute) }
    }

    private fun resetTimes() {
        val defaults = MealNotificationPreferences()
        viewModelScope.launch {
            NOTIFIABLE_MEAL_TYPES.forEach { mealType ->
                val (hour, minute) = defaults.timeFor(mealType)
                prefsRepository.updateMealTypeTime(mealType, hour, minute)
            }
        }
    }
}
