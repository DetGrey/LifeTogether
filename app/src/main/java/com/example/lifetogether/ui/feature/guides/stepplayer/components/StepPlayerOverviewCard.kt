package com.example.lifetogether.ui.feature.guides.stepplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.feature.guides.stepplayer.GuideStepPlayerUiState
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun StepPlayerOverviewCard(uiState: GuideStepPlayerUiState.Content) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)
            ) {
                TextHeadingMedium(uiState.sectionTitle)

                Text(
                    modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall),
                    text = "(${uiState.sectionSubtitle})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (uiState.currentPartLabel.isNotBlank()) {
                Text(
                    text = uiState.currentPartLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { uiState.currentPartProgressPercent / 100f },
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            )

            Text(
                text = "Part progress: ${uiState.currentPartProgressText} (${uiState.currentPartProgressPercent}%)",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Step ${uiState.currentStepNumber} of ${uiState.totalSteps}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StepPlayerOverviewCardPreview() {
    LifeTogetherTheme {
        StepPlayerOverviewCard(
            uiState = GuideStepPlayerUiState.Content(
                guide = null,
                currentStep = null,
                nextStep = null,
                currentRoundGroupLabel = "",
                currentRoundGroupMeta = "",
                sectionTitle = "Ears",
                sectionSubtitle = "Make 2",
                currentPartLabel = "Part 1/2",
                currentPartProgressPercent = 60,
                currentPartProgressText = "3 / 5",
                currentStepNumber = 4,
                totalSteps = 139,
            ),
        )
    }
}
