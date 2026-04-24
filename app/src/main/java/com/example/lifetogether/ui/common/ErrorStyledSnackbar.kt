package com.example.lifetogether.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun ErrorStyledSnackbar(
    data: SnackbarData,
) {
    Snackbar(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = Color.White,
        actionOnNewLine = true,
        action = {
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier
                    .padding(8.dp)
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
                color = Color.White,
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp),
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
