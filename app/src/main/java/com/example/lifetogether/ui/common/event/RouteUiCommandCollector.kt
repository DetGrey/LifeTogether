package com.example.lifetogether.ui.common.event

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalInspectionMode
import kotlinx.coroutines.flow.Flow

val LocalRootSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalRootSnackbarHostState was not provided")
}

@Composable
fun CollectUiCommands(
    commands: Flow<UiCommand>,
) {
    if (LocalInspectionMode.current) return

    val snackbarHostState = LocalRootSnackbarHostState.current
    LaunchedEffect(commands, snackbarHostState) {
        commands.collect { command ->
            when (command) {
                is UiCommand.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = command.message,
                    actionLabel = command.actionLabel,
                    withDismissAction = command.withDismissAction,
                    duration = command.duration,
                )
            }
        }
    }
}
