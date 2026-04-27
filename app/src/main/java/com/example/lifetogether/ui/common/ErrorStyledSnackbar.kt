package com.example.lifetogether.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ErrorStyledSnackbar(
    data: SnackbarData,
) {
    Snackbar(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        actionOnNewLine = true,
        action = {
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.small)
                    .clickable { data.dismiss() },
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "An error occurred",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onError,
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(top = LifeTogetherTokens.spacing.small),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStyledSnackbarPreview() {
    LifeTogetherTheme {
        ErrorStyledSnackbar(
            data = object : SnackbarData {
                override val visuals: SnackbarVisuals = object : SnackbarVisuals {
                    override val message: String = "Please enter a suggestion first"
                    override val actionLabel: String? = null
                    override val withDismissAction: Boolean = true
                    override val duration: SnackbarDuration = SnackbarDuration.Short
                }

                override fun dismiss() = Unit
                override fun performAction() = Unit
            },
        )
    }
}
