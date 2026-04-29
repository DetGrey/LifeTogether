package com.example.lifetogether.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun <T> ConfirmationDialogWithDropdown(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogMessage: String,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
    selectedValue: T,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<T>,
    onValueChange: (T) -> Unit,
    optionLabel: (T) -> String,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium)
            ) {
                TextDefault(text = dialogMessage)
                Dropdown(
                    selectedValue = optionLabel(selectedValue),
                    expanded = expanded,
                    onExpandedChange = onExpandedChange,
                    options = options.map(optionLabel),
                    label = null,
                    onValueChangedEvent = { selectedLabel ->
                        options.firstOrNull { optionLabel(it) == selectedLabel }?.let(onValueChange)
                    },
                )
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
            optionLabel = { it },
        )
    }
}
