package com.example.lifetogether.ui.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
fun TextSubHeadingMedium(
    text: String,
    color: Color = MaterialTheme.colorScheme.secondary,
    alignCenter: Boolean = false
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        textAlign = if (alignCenter) TextAlign.Center else TextAlign.Unspecified
    )
}
