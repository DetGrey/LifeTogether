package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun GuideSectionCard(
    section: GuideSection,
    selectedPieceIndex: Int,
    onSelectPieceIndex: (Int) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    canToggleStep: (Int) -> Boolean,
    onToggleStep: (Int, String) -> Unit,
) {
    val normalizedPieces = section.pieces.coerceAtLeast(1)
    val normalizedSelectedPieceIndex = selectedPieceIndex.coerceIn(0, normalizedPieces - 1)
    val progress = GuideProgress.sectionProgress(section)
    val progressPercent = GuideProgress.progressPercent(section)
    val selectedPieceProgress = GuideProgress.sectionPieceProgress(
        section = section,
        pieceIndex = normalizedSelectedPieceIndex,
    )
    val selectedPieceSteps = GuideProgress.sectionStepsForPiece(
        section = section,
        pieceIndex = normalizedSelectedPieceIndex,
    )
    val pieceOptions = (0 until normalizedPieces).map { pieceIndex ->
        val pieceProgress = GuideProgress.sectionPieceProgress(section, pieceIndex)
        "Piece ${pieceIndex + 1} (${pieceProgress.first}/${pieceProgress.second})"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
                    TextSubHeadingMedium(
                        text = buildString {
                            append(section.title)
                            if (section.pieces > 1) {
                                append(" (×${section.pieces})")
                            }
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    TextDefault(
                        text = buildString {
                            append("${progress.first}/${progress.second} steps completed")
                            if (normalizedPieces > 1) {
                                append(" • ${selectedPieceProgress.first}/${selectedPieceProgress.second} in selected piece")
                            }
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Icon(
                    modifier = Modifier.height(25.dp),
                    painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                    contentDescription = if (expanded) "collapse section" else "expand section",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { progressPercent / 100f },
                trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                drawStopIndicator = {},
            )

            if (!section.comment.isNullOrBlank()) {
                CommentBubble(
                    comment = section.comment,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    surfaceColor = MaterialTheme.colorScheme.surfaceVariant,
                    label = "Section note",
                    indentLevel = 0,
                )
            }

            if (normalizedPieces > 1) {
                TagOptionRow(
                    options = pieceOptions,
                    selectedOption = pieceOptions[normalizedSelectedPieceIndex],
                    onSelectedOptionChange = { selectedOption ->
                        val selectedIndex = pieceOptions.indexOf(selectedOption)
                        if (selectedIndex != -1) {
                            onSelectPieceIndex(selectedIndex)
                        }
                    },
                    showDividers = false
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                val canToggleSelectedPiece = canToggleStep(normalizedSelectedPieceIndex)
                GuideStepRows(
                    steps = selectedPieceSteps,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indentLevel = 0,
                    canToggleStep = canToggleSelectedPiece,
                    onToggleStep = { stepId ->
                        onToggleStep(normalizedSelectedPieceIndex, stepId)
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    LifeTogetherTheme {
        GuideSectionCard(
            section = GuideSection(
                title = "Title",
                subtitle = "subtitle",
                pieces = 10,
                completedPieces = 4,
                comment = "comment......",
                steps = listOf(
                    GuideStep(
                        id = "step-1",
                        name = "Step 1",
                        type = GuideStepType.NUMBERED,
                        title = "",
                        content = "Example step",
                        subSteps = emptyList(),
                    ),
                )
            ),
            selectedPieceIndex = 1,
            onSelectPieceIndex = {},
            expanded = true,
            onToggleStep = { _, _ -> },
            canToggleStep = { true },
            onToggleExpanded = {},
        )
    }
}
