package com.example.lifetogether.ui.common.event

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import com.example.lifetogether.ui.common.snackbar.AppSnackbarVisuals
import kotlinx.coroutines.flow.Flow

val LocalRootSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalRootSnackbarHostState was not provided")
}

interface ProgressSnackbarController {
    fun show(visuals: AppSnackbarVisuals)
    fun hide()
}

val LocalProgressSnackbarController = staticCompositionLocalOf<ProgressSnackbarController> {
    error("LocalProgressSnackbarController was not provided")
}

@Composable
fun CollectUiCommands(
    commands: Flow<UiCommand>,
) {
    if (LocalInspectionMode.current) return

    val snackbarHostState = LocalRootSnackbarHostState.current
    val progressSnackbarController = LocalProgressSnackbarController.current
    LaunchedEffect(commands, snackbarHostState, progressSnackbarController) {
        commands.collect { command ->
            when (command) {
                is UiCommand.ShowSnackbar -> snackbarHostState.showSnackbar(
                    visuals = AppSnackbarVisuals(
                        message = command.message,
                        actionLabel = command.actionLabel,
                        withDismissAction = command.withDismissAction,
                        duration = command.duration,
                        title = command.title,
                        severity = command.severity,
                        showProgress = command.showProgress,
                    ),
                )

                is UiCommand.ShowProgressSnackbar -> progressSnackbarController.show(
                    AppSnackbarVisuals(
                        message = command.message,
                        title = command.title,
                        severity = command.severity,
                        showProgress = command.showProgress,
                        withDismissAction = true,
                    ),
                )

                UiCommand.HideProgressSnackbar -> progressSnackbarController.hide()
            }
        }
    }
}
