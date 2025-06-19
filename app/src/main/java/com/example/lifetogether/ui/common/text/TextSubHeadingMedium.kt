package com.example.lifetogether.ui.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun TextSubHeadingMedium(
    text: String,
    color: Color = MaterialTheme.colorScheme.secondary,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = color,
    )
}
