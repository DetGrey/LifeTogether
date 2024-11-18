package com.example.lifetogether.ui.common.text

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.wear.compose.material.ContentAlpha

@Composable
fun EditableTextField(
    text: String,
    onTextChange: (String) -> Unit,
    label: String,
    isEditable: Boolean,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Black,
) {
    if (isEditable) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = onTextChange,
            label = { Text(label) },
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
