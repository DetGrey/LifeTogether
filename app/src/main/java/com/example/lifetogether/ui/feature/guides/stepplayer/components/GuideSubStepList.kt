package com.example.lifetogether.ui.feature.guides.stepplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun GuideSubStepList(
    steps: List<GuideStep>,
    textColor: Color,
    indentLevel: Int,
) {
    var numberedIndex = 1
    Column(
        modifier = Modifier.padding(start = (indentLevel * 14).dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        steps.forEach { step ->
            when (step.type) {
                GuideStepType.ROUND -> {
                    GuideStepRoundText(
                        title = guideStepRoundLabel(step),
                        content = step.content,
                        textColor
                    )
                }

                GuideStepType.COMMENT, GuideStepType.UNKNOWN -> {
                    GuideStepCommentText(
                        guideStepCommentText(step),
                        textColor
                    )
                }

                GuideStepType.NUMBERED -> {
                    GuideStepNumberedText(
                        index = numberedIndex,
                        text = guideStepNumberedText(step),
                        textColor
                    )
                    numberedIndex += 1
                }

                GuideStepType.SUBSECTION -> {
                    Text(
                        text = guideStepSubsectionLabel(step),
                        modifier = Modifier.padding(start = (indentLevel * 14).dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (step.subSteps.isNotEmpty()) {
                        GuideSubStepList(
                            steps = step.subSteps,
                            textColor = textColor,
                            indentLevel = indentLevel + 1,
                        )
                    } else if (step.content.isNotBlank()) {
                        Text(
                            text = step.content,
                            modifier = Modifier.padding(start = ((indentLevel + 1) * 14).dp),
                            color = textColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideSubStepListPreview() {
    LifeTogetherTheme {
        GuideSubStepList(
            steps = listOf(
                GuideStep(
                    id = "round-1",
                    type = GuideStepType.ROUND,
                    name = "R1",
                    content = "Start with appreciation.",
                ),
                GuideStep(
                    id = "numbered-1",
                    type = GuideStepType.NUMBERED,
                    content = "Say one thing you need this week.",
                ),
                GuideStep(
                    id = "subsection-1",
                    type = GuideStepType.SUBSECTION,
                    title = "Wrap up",
                    subSteps = listOf(
                        GuideStep(
                            id = "comment-1",
                            type = GuideStepType.COMMENT,
                            content = "End with a short hug.",
                        ),
                    ),
                ),
            ),
            textColor = MaterialTheme.colorScheme.onSurface,
            indentLevel = 1,
        )
    }
}
