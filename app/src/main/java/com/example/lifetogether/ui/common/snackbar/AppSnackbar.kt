package com.example.lifetogether.ui.common.snackbar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

enum class SnackbarSeverity {
    Error,
    Info,
}

data class AppSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: androidx.compose.material3.SnackbarDuration = androidx.compose.material3.SnackbarDuration.Short,
    val title: String? = null,
    val severity: SnackbarSeverity = SnackbarSeverity.Error,
    val showProgress: Boolean = false,
) : SnackbarVisuals

@Composable
fun AppSnackbar(
    data: SnackbarData,
) {
    val visuals = data.visuals as? AppSnackbarVisuals
    val severity = visuals?.severity ?: SnackbarSeverity.Error
    val title = visuals?.title ?: defaultTitle(severity)
    val colors = snackbarColors(severity)

    Snackbar(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        actionOnNewLine = true,
        action = {
            if (visuals?.withDismissAction == true) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.contentColor,
                    modifier = Modifier
                        .padding(LifeTogetherTokens.spacing.small)
                        .clickable { data.dismiss() },
                )
            }
        },
    ) {
        AppSnackbarContent(
            title = title,
            message = data.visuals.message,
            severity = severity,
            showProgress = visuals?.showProgress == true,
        )
    }
}

@Composable
fun AppSnackbar(
    title: String,
    message: String,
    severity: SnackbarSeverity,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = snackbarColors(severity)

    Snackbar(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = colors.containerColor,
        contentColor = colors.contentColor,
        actionOnNewLine = true,
        action = {
            if (onDismiss != null) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.contentColor,
                    modifier = Modifier
                        .padding(LifeTogetherTokens.spacing.small)
                        .clickable { onDismiss() },
                )
            }
        },
    ) {
        AppSnackbarContent(
            title = title,
            message = message,
            severity = severity,
            showProgress = showProgress,
        )
    }
}

private data class SnackbarColors(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
)

@Composable
private fun snackbarColors(severity: SnackbarSeverity): SnackbarColors {
    return when (severity) {
        SnackbarSeverity.Error -> SnackbarColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )

        SnackbarSeverity.Info -> SnackbarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun defaultTitle(severity: SnackbarSeverity): String {
    return when (severity) {
        SnackbarSeverity.Error -> "An error occurred"
        SnackbarSeverity.Info -> "Notice"
    }
}

@Composable
private fun AppSnackbarContent(
    title: String,
    message: String,
    severity: SnackbarSeverity,
    showProgress: Boolean,
) {
    val colors = snackbarColors(severity)
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.contentColor,
        )
        if (showProgress) {
            Spacer(modifier = Modifier.size(LifeTogetherTokens.spacing.small))
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colors.contentColor,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.contentColor,
            modifier = Modifier.padding(top = LifeTogetherTokens.spacing.small),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppSnackbarPreview() {
    LifeTogetherTheme {
        AppSnackbar(
            title = "Downloading...",
            message = "Downloading 1 of 3",
            severity = SnackbarSeverity.Info,
            showProgress = true,
            onDismiss = {},
        )
    }
}
