package com.example.lifetogether.ui.common.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
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
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                    label = label,
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                    capitalization = capitalization,
                    modifier = Modifier.focusRequester(focusRequester),
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
            label = "Name",
        )
    }
}
