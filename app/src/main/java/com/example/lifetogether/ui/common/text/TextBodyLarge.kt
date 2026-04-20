package com.example.lifetogether.ui.common.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun TextBodyLarge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = color ?: MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}
