package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CommentBubble(
    comment: String,
    textColor: Color,
    surfaceColor: Color,
    label: String,
    indentLevel: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ((indentLevel * 18) + 40).dp)
            .background(surfaceColor, MaterialTheme.shapes.small)
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = comment,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}
