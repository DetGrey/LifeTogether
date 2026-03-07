package com.example.lifetogether.ui.feature.guides.stepplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun GuideStepCard(
    header: String,
    step: GuideStep?,
    stepNumber: Int?,
    roundGroupLabel: String = "",
    roundGroupMeta: String = "",
    emphasized: Boolean,
) {
    val cardColor = if (emphasized) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (emphasized) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val modifier = if (emphasized) Modifier.height(275.dp) else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
            if (step == null) {
                TextDefault(
                    text = "No step available",
                    color = textColor,
                )
            } else {
                GuideStepCardBody(
                    step = step,
                    stepNumber = stepNumber,
                    textColor = textColor,
                    roundGroupLabel = roundGroupLabel,
                    roundGroupMeta = roundGroupMeta,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideStepCardPreview() {
    LifeTogetherTheme {
        GuideStepCard(
            header = "Current step",
            step = GuideStep(
                id = "step-1",
                type = GuideStepType.NUMBERED,
                content = "Share one thing that felt good this week.",
            ),
            stepNumber = 3,
            emphasized = true,
        )
        GuideStepCard(
            header = "Current step",
            step = GuideStep(
                id = "step-1",
                type = GuideStepType.ROUND,
                title = "R1",
                content = "Share one thing that felt good this week.",
            ),
            stepNumber = 3,
            emphasized = true,
        )
    }
}
