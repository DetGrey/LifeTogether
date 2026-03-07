package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.list.CompletableBox

@Composable
fun StepToggleRow(
    text: String,
    isCompleted: Boolean,
    enabled: Boolean,
    textColor: Color,
    indentLevel: Int,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 18).dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.alpha(if (enabled) 1f else 0.45f)) {
            CompletableBox(
                isCompleted = isCompleted,
                onCompleteToggle = {
                    if (enabled) {
                        onToggle()
                    }
                },
            )
        }

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}