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
import com.example.lifetogether.ui.theme.LifeTogetherTokens

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
            .padding(start = (LifeTogetherTokens.spacing.large * indentLevel) + LifeTogetherTokens.spacing.xxLarge)
            .background(surfaceColor, MaterialTheme.shapes.small)
            .padding(LifeTogetherTokens.spacing.small),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
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
