package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.feature.guides.stepplayer.components.GuideStepRowText

@Composable
fun GuideStepRows(
    steps: List<GuideStep>,
    textColor: Color,
    indentLevel: Int,
    canToggleStep: Boolean,
    onToggleStep: (String) -> Unit,
) {
    var numberedIndex = 1

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        steps.forEach { step ->
            val currentNumberedIndex = numberedIndex

            StepToggleRow(
                isCompleted = step.completed,
                enabled = canToggleStep,
                indentLevel = indentLevel,
                onToggle = { onToggleStep(step.id) },
            ) { modifier, textDecoration ->
                GuideStepRowText(
                    step = step,
                    textColor = textColor,
                    modifier = modifier,
                    textDecoration = textDecoration,
                    numberedIndex = if (step.type == GuideStepType.NUMBERED) currentNumberedIndex else null,
                )
            }

            if (step.type == GuideStepType.NUMBERED) {
                numberedIndex += 1
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
    }
}
