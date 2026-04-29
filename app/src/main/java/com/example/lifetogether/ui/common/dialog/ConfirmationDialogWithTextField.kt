package com.example.lifetogether.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ConfirmationDialogWithTextField(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogMessage: String,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
    textValue: String,
    onTextValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium)
            ) {
                Text(text = dialogMessage)
                CustomTextField(
                    value = textValue,
                    onValueChange = onTextValueChange,
                    label = null,
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                    capitalization = capitalization,
                )
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text(
                    text = dismissButtonMessage,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = confirmButtonMessage,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    )
}

@Preview
@Composable
fun ConfirmationDialogWithTextFieldPreview() {
    LifeTogetherTheme {
        ConfirmationDialogWithTextField(
            onDismiss = { },
            onConfirm = { },
            dialogTitle = "Change name",
            dialogMessage = "Please enter your new name",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Change name",
            textValue = "",
            onTextValueChange = { },
        )
    }
}
