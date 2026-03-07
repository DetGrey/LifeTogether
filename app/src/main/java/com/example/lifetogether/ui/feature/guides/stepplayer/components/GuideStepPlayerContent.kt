package com.example.lifetogether.ui.feature.guides.stepplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.feature.guides.stepplayer.GuideStepPlayerUiState
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun GuideStepPlayerContent(
    uiState: GuideStepPlayerUiState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onCompleteCurrentAndGoNext: () -> Unit,
    onToggleCurrentStepCompletion: () -> Unit,
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
                onLeftClick = onBack,
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
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canGoPrevious,
                    onClick = onPrevious,
                ) {
                    Text("Previous")
                }

                Button(
                    modifier = Modifier.weight(1.35f),
                    enabled = canPrimaryAction,
                    onClick = onCompleteCurrentAndGoNext,
                ) {
                    Text(
                        text = when {
                            uiState.currentStepCompleted && uiState.canGoNext -> "Next step"
                            uiState.currentStepCompleted -> "Completed"
                            uiState.canGoNext -> "Complete + next"
                            else -> "Complete step"
                        },
                    )
                }
            }
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.currentStep != null && uiState.canToggleCurrentStep,
                onClick = onToggleCurrentStepCompletion,
            ) {
                Text(
                    text = if (uiState.currentStepCompleted) {
                        "Mark current step incomplete"
                    } else {
                        "Mark current step complete"
                    },
                )
            }
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
        GuideStepPlayerContent(
            uiState = GuideStepPlayerUiState(
                currentStep = GuideStep(
                    id = "step-3",
                    type = GuideStepType.SUBSECTION,
                    title = "Check-in",
                    subSteps = listOf(
                        GuideStep(
                            id = "step-3a",
                            type = GuideStepType.COMMENT,
                            content = "Take a breath and sit close.",
                        ),
                        GuideStep(
                            id = "step-3b",
                            type = GuideStepType.NUMBERED,
                            content = "Share one highlight from the week.",
                        ),
                    ),
                ),
                nextStep = GuideStep(
                    id = "step-4",
                    type = GuideStepType.ROUND,
                    name = "Round 2",
                    content = "Talk about what you need next week.",
                ),
                currentStepCompleted = false,
                canToggleCurrentStep = true,
                currentRoundGroupLabel = "Round 1",
                currentRoundGroupMeta = "1 of 2 rounds",
                currentStepNumber = 3,
                totalSteps = 6,
                sectionTitle = "Ears",
                sectionSubtitle = "Make 2",
                currentPartLabel = "Part 2/3",
                currentPartProgressPercent = 33,
                currentPartProgressText = "1 / 3",
                canGoPrevious = true,
                canGoNext = true,
            ),
            onBack = {},
            onPrevious = {},
            onCompleteCurrentAndGoNext = {},
            onToggleCurrentStepCompletion = {},
        )
    }
}
