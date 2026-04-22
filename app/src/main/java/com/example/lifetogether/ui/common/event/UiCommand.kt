package com.example.lifetogether.ui.common.event

import androidx.compose.material3.SnackbarDuration

sealed interface UiCommand {
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : UiCommand
}
