package com.example.lifetogether.ui.common.text

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun TextHeadingLarge(
    text: String,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = text,
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
}
