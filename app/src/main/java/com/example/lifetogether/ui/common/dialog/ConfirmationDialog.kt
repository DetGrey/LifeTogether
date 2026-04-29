package com.example.lifetogether.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogMessage: String,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
    content: @Composable () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium)
            ) {
                Text(text = dialogMessage)
                content()
            }
        },
        dismissButton = {
            SecondaryButton(
                text = dismissButtonMessage,
                onClick = onDismiss,
            )
        },
        confirmButton = {
            PrimaryButton(
                text = confirmButtonMessage,
                onClick = onConfirm,
            )
        },
    )
}
