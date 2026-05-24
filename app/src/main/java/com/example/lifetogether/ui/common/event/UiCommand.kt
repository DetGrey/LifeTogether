package com.example.lifetogether.ui.common.event

import androidx.compose.material3.SnackbarDuration
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity

sealed interface UiCommand {
    data class ShowSnackbar(
        val message: String,
        val title: String? = null,
        val severity: SnackbarSeverity = SnackbarSeverity.Error,
        val showProgress: Boolean = false,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : UiCommand

    data class ShowProgressSnackbar(
        val title: String,
        val message: String,
        val severity: SnackbarSeverity = SnackbarSeverity.Info,
        val showProgress: Boolean = true,
        val autoDismissAfterMillis: Long? = null,
    ) : UiCommand

    data object HideProgressSnackbar : UiCommand
}
