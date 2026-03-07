package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep

@Composable
fun GuideSectionCard(
    section: GuideSection,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    canToggleStep: (String) -> Boolean,
    onToggleStep: (String) -> Unit,
) {
    val progress = GuideProgress.sectionProgress(section)
    val progressPercent = GuideProgress.progressPercent(section)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = buildString {
                            append(section.title)
                            if (section.amount > 1) {
                                append(" (x${section.amount})")
                            }
                        },
                        color = MaterialTheme.colorScheme.background,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${progress.first}/${progress.second} steps completed • ${countLeafSteps(section.steps)} steps",
                        color = MaterialTheme.colorScheme.background,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Icon(
                    modifier = Modifier.height(25.dp),
                    painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                    contentDescription = if (expanded) "collapse section" else "expand section",
                    tint = MaterialTheme.colorScheme.background,
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progressPercent / 100f },
                trackColor = MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
            )

            if (!section.comment.isNullOrBlank()) {
                CommentBubble(
                    comment = section.comment,
                    textColor = MaterialTheme.colorScheme.background,
                    surfaceColor = MaterialTheme.colorScheme.background.copy(alpha = 0.14f),
                    label = "Section note",
                    indentLevel = 0,
                )
            }

            if (expanded) {
                GuideStepRows(
                    steps = section.steps,
                    textColor = MaterialTheme.colorScheme.background,
                    indentLevel = 0,
                    canToggleStep = canToggleStep,
                    onToggleStep = onToggleStep,
                )
            }
        }
    }
}

private fun countLeafSteps(steps: List<GuideStep>): Int {
    return steps.sumOf { step ->
        if (step.subSteps.isNotEmpty()) {
            countLeafSteps(step.subSteps)
        } else {
            1
        }
    }
}
