package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.list.CompletableBox

@Composable
fun StepToggleRow(
    isCompleted: Boolean,
    enabled: Boolean,
    indentLevel: Int,
    onToggle: () -> Unit,
    content: @Composable (modifier: Modifier, textDecoration: TextDecoration) -> Unit,
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

        content(
            Modifier.weight(1f),
            if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}
