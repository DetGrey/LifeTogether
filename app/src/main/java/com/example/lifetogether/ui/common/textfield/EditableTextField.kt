package com.example.lifetogether.ui.common.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.wear.compose.material.ContentAlpha

@Composable
fun EditableTextField(
    text: String,
    onTextChange: (String) -> Unit,
    label: String,
    isEditable: Boolean,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Black,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    capitalization: Boolean = true,
) {
    if (isEditable) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = onTextChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = if (capitalization) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            ),
            textStyle = textStyle.copy(color = color),
            colors = TextFieldDefaults.colors(
                focusedTextColor = color,
                unfocusedTextColor = color,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = color,
                errorCursorColor = MaterialTheme.colorScheme.error,
            ),
        )
    } else {
        Text(
            text = text,
            style = textStyle,
            color = color,
        )
    }
}
