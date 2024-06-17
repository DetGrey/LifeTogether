package com.example.lifetogether.ui.common.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.Dropdown
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun ConfirmationDialogWithDropdown(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogMessage: String,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onValueChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Text(text = dialogMessage)
            Dropdown(
                selectedValue = selectedValue,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                options = options,
                label = null,
                onValueChangedEvent = onValueChange,
            )
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = Color.White,
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
                    contentColor = Color.White,
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
fun ConfirmationDialogWithDropdownPreview() {
    LifeTogetherTheme {
        ConfirmationDialogWithDropdown(
            onDismiss = { },
            onConfirm = { },
            dialogTitle = "Select category",
            dialogMessage = "",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Confirm",
            selectedValue = "Select category",
            expanded = true,
            onExpandedChange = { },
            options = listOf("Select category", "Cat1", "cat2"),
            onValueChange = {},
        )
    }
}
