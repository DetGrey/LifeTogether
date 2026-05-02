package com.example.lifetogether.ui.common.textfield

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppInputHeight = 60.dp

@Composable
fun Modifier.inputFieldModifier(): Modifier {
    return fillMaxWidth()
        .height(AppInputHeight)
        .clip(shape = MaterialTheme.shapes.large)
}

@Composable
fun Modifier.editableInputFieldModifier(): Modifier {
    return fillMaxWidth()
        .clip(shape = MaterialTheme.shapes.large)
}

@Composable
fun filledTextFieldColors(): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
        focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
        cursorColor = MaterialTheme.colorScheme.secondary,
        errorCursorColor = MaterialTheme.colorScheme.error,
    )
}

@Composable
fun transparentTextFieldColors(
    textColor: Color = MaterialTheme.colorScheme.onBackground,
): TextFieldColors {
    return TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedIndicatorColor =Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = textColor,
        errorCursorColor = MaterialTheme.colorScheme.error,
    )
}
