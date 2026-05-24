package com.example.lifetogether.ui.feature.settings.notifications

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.notification.MealNotificationPreferences
import com.example.lifetogether.domain.model.notification.formatTimeFor
import com.example.lifetogether.domain.model.notification.timeFor
import com.example.lifetogether.domain.model.notification.typeEnabledFor
import com.example.lifetogether.domain.notification.NOTIFIABLE_MEAL_TYPES
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import androidx.core.net.toUri
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.text.TextHeadingLarge
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium

@Composable
fun NotificationsScreen(
    uiState: NotificationsUiState,
    onUiEvent: (NotificationsUiEvent) -> Unit,
    onNavigationEvent: (NotificationsNavigationEvent) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onUiEvent(NotificationsUiEvent.RefreshPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(resId = R.drawable.ic_back, description = "back arrow icon"),
                onLeftClick = { onNavigationEvent(NotificationsNavigationEvent.NavigateBack) },
                text = "Notifications",
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is NotificationsUiState.Loading,
            label = "notification_settings_loading",
            loadingContent = { Skeletons.ListDetail(modifier = Modifier.fillMaxSize()) },
        ) {
            val content = uiState as? NotificationsUiState.Content
                ?: return@AnimatedLoadingContent

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LifeTogetherTokens.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            ) {
                item {
                    TextDefault(
                        text = "Manage notification channels >",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(LifeTogetherTokens.spacing.small)
                            .clip(MaterialTheme.shapes.large)
                            .clickable { openSystemNotificationSettings(context) }
                            .padding(LifeTogetherTokens.spacing.medium),
                        textAlign = TextAlign.Center,
                    )
                }

                item {
                    TextHeadingLarge(
                        text = "Meal Plans",
                        textAlign = TextAlign.Start
                    )
                }

                if (!content.hasExactAlarmPermission) {
                    item {
                        PermissionWarningBanner(
                            onClick = { openExactAlarmSettings(context) },
                        )
                    }
                }

                item {
                    MasterToggleRow(
                        enabled = content.prefs.masterEnabled,
                        onToggle = {
                            if (!content.prefs.masterEnabled && !content.hasExactAlarmPermission) {
                                openExactAlarmSettings(context)
                            } else {
                                onUiEvent(NotificationsUiEvent.ToggleMaster)
                            }
                        },
                    )
                }

                item {
                    TextSubHeadingMedium(
                        text = "Reminder times",
                        modifier = Modifier.padding(
                            top = LifeTogetherTokens.spacing.medium,
                            bottom = LifeTogetherTokens.spacing.xSmall,
                        ),
                    )
                }

                items(NOTIFIABLE_MEAL_TYPES.size) { index ->
                    val mealType = NOTIFIABLE_MEAL_TYPES[index]
                    MealTypeRow(
                        mealType = mealType,
                        typeEnabled = content.prefs.typeEnabledFor(mealType),
                        formattedTime = content.prefs.formatTimeFor(mealType),
                        onToggle = { onUiEvent(NotificationsUiEvent.ToggleMealType(mealType)) },
                        onTimeClick = { onUiEvent(NotificationsUiEvent.ShowTimePicker(mealType)) },
                    )
                    if (index < NOTIFIABLE_MEAL_TYPES.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = LifeTogetherTokens.spacing.xSmall),
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }

                item {
                    SecondaryButton(
                        text = "Reset to defaults",
                        onClick = { onUiEvent(NotificationsUiEvent.ResetTimes) },
                        modifier = Modifier.padding(top = LifeTogetherTokens.spacing.small),
                    )
                }
            }

            content.timePickerFor?.let { mealType ->
                val (initialHour, initialMinute) = content.prefs.timeFor(mealType)
                TimePickerDialog(
                    initialHour = initialHour,
                    initialMinute = initialMinute,
                    onDismiss = { onUiEvent(NotificationsUiEvent.DismissTimePicker) },
                    onConfirm = { h, m ->
                        onUiEvent(NotificationsUiEvent.ConfirmTime(mealType, h, m))
                    },
                )
            }
        }
    }
}

@Composable
private fun PermissionWarningBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
        ) {
            TextDefault(
                text = "Alarm permission required",
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextLabel(
                text = "Reminders are off — tap to grant permission in system settings",
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun MasterToggleRow(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextDefault(text = "Meal plan reminders")
        Switch(checked = enabled, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun MealTypeRow(
    mealType: MealType,
    typeEnabled: Boolean,
    formattedTime: String,
    onToggle: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.xSmall),
        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = typeEnabled, onCheckedChange = { onToggle() })
        TextDefault(text = mealType.displayName, modifier = Modifier.weight(1f))
        TextDefault(
            text = formattedTime,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable { onTimeClick() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Select reminder time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun openExactAlarmSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = "package:${context.packageName}".toUri()
    }
    context.startActivity(intent)
}

private fun openSystemNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}

@Preview(showBackground = true)
@Composable
private fun NotificationsScreenPreview() {
    LifeTogetherTheme {
        NotificationsScreen(
            uiState = NotificationsUiState.Content(
                prefs = MealNotificationPreferences(masterEnabled = true),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsScreenNoPermissionPreview() {
    LifeTogetherTheme {
        NotificationsScreen(
            uiState = NotificationsUiState.Content(
                prefs = MealNotificationPreferences(masterEnabled = false),
                hasExactAlarmPermission = false,
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
