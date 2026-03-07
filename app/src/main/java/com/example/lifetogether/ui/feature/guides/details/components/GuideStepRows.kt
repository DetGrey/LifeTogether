package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType


@Composable
fun GuideStepRows(
    steps: List<GuideStep>,
    textColor: Color,
    indentLevel: Int,
    canToggleStep: (String) -> Boolean,
    onToggleStep: (String) -> Unit,
) {
    var numberedIndex = 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEach { step ->
            when (step.type) {
                GuideStepType.COMMENT -> {
                    CommentBubble(
                        comment = commentText(step),
                        textColor = textColor,
                        surfaceColor = textColor.copy(alpha = 0.12f),
                        label = "Comment",
                        indentLevel = indentLevel,
                    )
                }

                GuideStepType.SUBSECTION -> {
                    Text(
                        text = subsectionLabel(step),
                        modifier = Modifier.padding(start = (indentLevel * 18).dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    if (step.content.isNotBlank()) {
                        CommentBubble(
                            comment = step.content,
                            textColor = textColor,
                            surfaceColor = textColor.copy(alpha = 0.12f),
                            label = "Subsection note",
                            indentLevel = indentLevel + 1,
                        )
                    }

                    if (step.subSteps.isNotEmpty()) {
                        GuideStepRows(
                            steps = step.subSteps,
                            textColor = textColor,
                            indentLevel = indentLevel + 1,
                            canToggleStep = canToggleStep,
                            onToggleStep = onToggleStep,
                        )
                    }
                }

                GuideStepType.NUMBERED -> {
                    StepToggleRow(
                        text = "$numberedIndex. ${numberedText(step)}",
                        isCompleted = step.completed,
                        enabled = canToggleStep(step.id),
                        textColor = textColor,
                        indentLevel = indentLevel,
                        onToggle = { onToggleStep(step.id) },
                    )
                    numberedIndex += 1

                    if (step.subSteps.isNotEmpty()) {
                        GuideStepRows(
                            steps = step.subSteps,
                            textColor = textColor,
                            indentLevel = indentLevel + 1,
                            canToggleStep = canToggleStep,
                            onToggleStep = onToggleStep,
                        )
                    }
                }

                GuideStepType.ROUND -> {
                    StepToggleRow(
                        text = roundDisplayText(step),
                        isCompleted = step.completed,
                        enabled = canToggleStep(step.id),
                        textColor = textColor,
                        indentLevel = indentLevel,
                        onToggle = { onToggleStep(step.id) },
                    )
                }

                GuideStepType.UNKNOWN -> {
                    StepToggleRow(
                        text = commentText(step),
                        isCompleted = step.completed,
                        enabled = canToggleStep(step.id),
                        textColor = textColor,
                        indentLevel = indentLevel,
                        onToggle = { onToggleStep(step.id) },
                    )
                }
            }
        }
    }
}

private fun commentText(step: GuideStep): String {
    if (step.content.isNotBlank()) return step.content
    if (step.name.isNotBlank()) return step.name
    if (step.title.isNotBlank()) return step.title
    return "Comment"
}

private fun roundLabel(step: GuideStep): String {
    if (step.name.isNotBlank()) return step.name
    if (step.title.isNotBlank()) return step.title
    return "Round"
}

private fun roundDisplayText(step: GuideStep): String {
    val label = roundLabel(step)
    return if (step.content.isNotBlank()) "$label ${step.content}" else label
}

private fun subsectionLabel(step: GuideStep): String {
    if (step.title.isNotBlank()) return step.title
    if (step.name.isNotBlank()) return step.name
    return "Subsection"
}

private fun numberedText(step: GuideStep): String {
    if (step.content.isNotBlank()) return step.content
    if (step.title.isNotBlank()) return step.title
    if (step.name.isNotBlank()) return step.name
    return "Step"
}
