package com.example.lifetogether.ui.common.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun EditableTextField(
    text: String,
    onTextChange: (String) -> Unit,
    label: String,
    isEditable: Boolean,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onBackground,
    labelColor: Color = MaterialTheme.colorScheme.onBackground,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    capitalization: Boolean = true,
    showLabelAsPlaceholder: Boolean = false,
    hideBackground: Boolean = false,
) {
    if (isEditable) {
        val labelContent: (@Composable () -> Unit)? = if (showLabelAsPlaceholder) {
            null
        } else {
            { Text(label, color = labelColor) }
        }
        val placeholderContent: (@Composable () -> Unit)? = if (showLabelAsPlaceholder) {
            { Text(
                text = label,
                color = labelColor,
                style = MaterialTheme.typography.bodySmall)
            }
        } else {
            null
        }

        TextField(
            modifier = Modifier.editableInputFieldModifier(),
            value = text,
            onValueChange = onTextChange,
            label = labelContent,
            placeholder = placeholderContent,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = if (capitalization) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            ),
            textStyle = textStyle.copy(color = color),
            colors = if (hideBackground) transparentTextFieldColors(textColor = color)
                else fadedTextFieldColors(textColor = color),
        )
    } else {
        Text(
            text = text,
            style = textStyle,
            color = color,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
