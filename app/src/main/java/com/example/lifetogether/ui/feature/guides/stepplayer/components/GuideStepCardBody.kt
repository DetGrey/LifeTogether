package com.example.lifetogether.ui.feature.guides.stepplayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun GuideStepCardBody(
    step: GuideStep,
    stepNumber: Int?,
    textColor: Color,
    roundGroupLabel: String,
    roundGroupMeta: String, //todo whats this
) {
    when (step.type) {
        GuideStepType.ROUND -> {
            GuideStepRoundText(
                title = roundGroupLabel.ifEmpty { guideStepRoundLabel(step) }, //todo this is awful
                content = step.content,
                textColor
            )
//            if (roundGroupMeta.isNotBlank()) {
//                Text(
//                    text = roundGroupMeta,
//                    color = textColor,
//                    style = MaterialTheme.typography.bodySmall,
//                )
//            }
        }

        GuideStepType.COMMENT, GuideStepType.UNKNOWN -> {
            GuideStepCommentText(
                guideStepCommentText(step),
                textColor
            )
        }

        GuideStepType.NUMBERED -> {
            GuideStepNumberedText(
                index = stepNumber ?: 1,
                text = guideStepNumberedText(step),
                textColor
            )
        }

        GuideStepType.SUBSECTION -> {
            Text(
                text = guideStepSubsectionLabel(step),
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (step.subSteps.isNotEmpty()) {
                GuideSubStepList(
                    steps = step.subSteps,
                    textColor = textColor,
                    indentLevel = 1,
                )
            } else if (step.content.isNotBlank()) {
                Text(
                    text = step.content,
                    modifier = Modifier.padding(start = LifeTogetherTokens.spacing.medium),
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideStepCardBodyPreview() {
    LifeTogetherTheme {
        Column() {
            GuideStepCardBody(
                step = GuideStep(
                    id = "subsection-1",
                    type = GuideStepType.SUBSECTION,
                    title = "Ears",
                    subSteps = listOf(
                        GuideStep(
                            id = "comment-1",
                            type = GuideStepType.COMMENT,
                            content = "Take a breath before you start.",
                        ),
                        GuideStep(
                            id = "numbered-1",
                            type = GuideStepType.ROUND,
                            title = "R1",
                            content = "Describe your mood in one sentence.",
                        ),
                        GuideStep(
                            id = "numbered-1",
                            type = GuideStepType.NUMBERED,
                            content = "Describe your mood in one sentence.",
                        ),
                    ),
                ),
                stepNumber = 2,
                textColor = MaterialTheme.colorScheme.onSurface,
                roundGroupLabel = "",
                roundGroupMeta = "",
            )
            GuideStepCardBody(
                step = GuideStep(
                    id = "numbered-1",
                    type = GuideStepType.ROUND,
                    title = "R1",
                    content = "Describe your mood in one sentence.",
                ),
                stepNumber = 2,
                textColor = MaterialTheme.colorScheme.onSurface,
                roundGroupLabel = "",
                roundGroupMeta = "",
            )
            GuideStepCardBody(
                step = GuideStep(
                    id = "numbered-1",
                    type = GuideStepType.NUMBERED,
                    content = "Describe your mood in one sentence.",
                ),
                stepNumber = 2,
                textColor = MaterialTheme.colorScheme.onSurface,
                roundGroupLabel = "",
                roundGroupMeta = "",
            )
        }
    }
}
