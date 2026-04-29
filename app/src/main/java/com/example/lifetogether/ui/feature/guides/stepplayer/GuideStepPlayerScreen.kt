package com.example.lifetogether.ui.feature.guides.stepplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.feature.guides.stepplayer.components.GuideStepCard
import com.example.lifetogether.ui.feature.guides.stepplayer.components.StepPlayerOverviewCard
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun GuideStepPlayerScreen(
    uiState: GuideStepPlayerUiState,
    onUiEvent: (GuideStepPlayerUiEvent) -> Unit,
    onNavigationEvent: (GuideStepPlayerNavigationEvent) -> Unit,
) {
    val canPrimaryAction = uiState.currentStep != null && (uiState.canGoNext || uiState.canToggleCurrentStep)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(GuideStepPlayerNavigationEvent.NavigateBack) },
                text = "Step player",
            )
        }

        item {
            StepPlayerOverviewCard(uiState)
        }

        item {
            GuideStepCard(
                header = "Current step",
                step = uiState.currentStep,
                stepNumber = uiState.currentStepNumber,
                roundGroupLabel = uiState.currentRoundGroupLabel,
                roundGroupMeta = uiState.currentRoundGroupMeta,
                emphasized = true,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryButton(
                    modifier = Modifier.weight(1f),
                    text = "Previous",
                    enabled = uiState.canGoPrevious,
                    onClick = { onUiEvent(GuideStepPlayerUiEvent.PreviousClicked) },
                )

                PrimaryButton(
                    modifier = Modifier.weight(1.35f),
                    text = when {
                        uiState.currentStepCompleted && uiState.canGoNext -> "Next step"
                        uiState.currentStepCompleted -> "Completed"
                        uiState.canGoNext -> "Complete + next"
                        else -> "Complete step"
                    },
                    enabled = canPrimaryAction,
                    onClick = { onUiEvent(GuideStepPlayerUiEvent.CompleteCurrentAndGoNextClicked) },
                )
            }
        }

        item {
            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (uiState.currentStepCompleted) {
                    "Mark current step incomplete"
                } else {
                    "Mark current step complete"
                },
                enabled = uiState.currentStep != null && uiState.canToggleCurrentStep,
                onClick = { onUiEvent(GuideStepPlayerUiEvent.ToggleCurrentStepCompletionClicked) },
            )
        }

        if (uiState.nextStep != null) {
            item {
                GuideStepCard(
                    header = "Up next",
                    step = uiState.nextStep,
                    stepNumber = (uiState.currentStepNumber + 1).coerceAtMost(uiState.totalSteps),
                    emphasized = false,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideStepPlayerScreenPreview() {
    LifeTogetherTheme {
        GuideStepPlayerScreen(
            uiState = GuideStepPlayerUiState(),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
