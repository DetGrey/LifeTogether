package com.example.lifetogether.ui.feature.guides.stepplayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.feature.guides.stepplayer.components.GuideStepCard
import com.example.lifetogether.ui.feature.guides.stepplayer.components.StepPlayerOverviewCard
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun GuideStepPlayerScreen(
    uiState: GuideStepPlayerUiState,
    onUiEvent: (GuideStepPlayerUiEvent) -> Unit,
    onNavigationEvent: (GuideStepPlayerNavigationEvent) -> Unit,
) {
    val canPrimaryAction = uiState.currentStep != null && (uiState.canGoNext || uiState.canToggleCurrentStep)

    Scaffold(
        topBar = {
            AppTopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(GuideStepPlayerNavigationEvent.NavigateBack) },
                text = "Step player",
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LifeTogetherTokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
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
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
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

            item {
                AnimatedVisibility(
                    visible = uiState.nextStep != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    uiState.nextStep?.let { nextStep ->
                        GuideStepCard(
                            header = "Up next",
                            step = nextStep,
                            stepNumber = (uiState.currentStepNumber + 1).coerceAtMost(uiState.totalSteps),
                            emphasized = false,
                        )
                    }
                }
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
