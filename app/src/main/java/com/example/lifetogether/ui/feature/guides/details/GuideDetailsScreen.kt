package com.example.lifetogether.ui.feature.guides.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.Icon as AppIcon
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.domain.model.guides.GuideVisibility
import com.example.lifetogether.ui.common.OverflowMenu
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GuideDetailsRoute(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
    guideId: String,
    viewModelStoreOwner: ViewModelStoreOwner? = null,
) {
    val guideDetailsViewModel: GuideDetailsViewModel = if (viewModelStoreOwner != null) {
        hiltViewModel(viewModelStoreOwner)
    } else {
        hiltViewModel()
    }

    GuideDetailsScreen(
        appNavigator = appNavigator,
        appSessionViewModel = appSessionViewModel,
        guideId = guideId,
        guideDetailsViewModel = guideDetailsViewModel,
    )
}

@Composable
fun GuideDetailsScreen(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
    guideId: String,
    guideDetailsViewModel: GuideDetailsViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    val uiState by guideDetailsViewModel.uiState.collectAsState()
    val guide = uiState.guide

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sectionExpandedState = remember(guide?.id) { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(userInformation?.familyId, userInformation?.uid, guideId) {
        val familyId = userInformation?.familyId
        val uid = userInformation?.uid
        if (!familyId.isNullOrBlank() && !uid.isNullOrBlank()) {
            guideDetailsViewModel.setUpGuide(familyId, uid, guideId)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TopBar(
                leftIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { appNavigator?.navigateBack() },
                text = "Guide details",
                rightIcon = AppIcon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = {
                    if (guide != null) {
                        showOverflowMenu = !showOverflowMenu
                    }
                },
            )
        }

        if (guide == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {

            item {
                GuideHeroCard(guide)
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isStartingGuide,
                    onClick = {
                        guideDetailsViewModel.onStartOrContinue { _ ->
                            appNavigator?.navigateToGuideStepPlayer()
                        }
                    },
                ) {
                    Text(
                        text = when {
                            uiState.isStartingGuide -> "Starting..."
                            guide.started -> "Continue where you left off"
                            else -> "Start guide"
                        },
                    )
                }
            }

            if (guide.sections.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(14.dp),
                    ) {
                        Text(
                            text = "No sections yet",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                guide.sections.forEachIndexed { sectionIndex, section ->
                    val sectionKey = section.id.ifBlank { "section-$sectionIndex" }
                    val isExpanded = sectionExpandedState.getOrPut(sectionKey) { true }

                    item(key = sectionKey) {
                        GuideSectionCard(
                            section = section,
                            expanded = isExpanded,
                            onToggleExpanded = {
                                sectionExpandedState[sectionKey] = !isExpanded
                            },
                            canToggleStep = { stepId -> guideDetailsViewModel.canToggleStep(stepId) },
                            onToggleStep = { stepId -> guideDetailsViewModel.toggleStepCompletion(stepId) },
                        )
                    }
                }
            }
        }
    }

    val currentGuide = guide
    if (showOverflowMenu && currentGuide != null) {
        val canModifyGuide = guideDetailsViewModel.canToggleVisibility()
        val visibilityActionLabel = if (currentGuide.visibility == GuideVisibility.FAMILY) {
            "Make private"
        } else {
            "Share with family"
        }

        OverflowMenu(
            onDismiss = { showOverflowMenu = false },
            actionsList = listOf(
                mapOf(visibilityActionLabel to {
                    showOverflowMenu = false
                    if (canModifyGuide) {
                        guideDetailsViewModel.toggleVisibility()
                    } else {
                        guideDetailsViewModel.showVisibilityOwnershipError()
                    }
                }),
                mapOf("Delete guide" to {
                    showOverflowMenu = false
                    if (canModifyGuide) {
                        showDeleteDialog = true
                    } else {
                        guideDetailsViewModel.showDeleteOwnershipError()
                    }
                }),
            ),
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                guideDetailsViewModel.deleteGuide {
                    appNavigator?.navigateBack()
                }
            },
            dialogTitle = "Delete guide",
            dialogMessage = "Are you sure you want to delete this guide?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete guide",
        )
    }

    if (uiState.showAlertDialog) {
        LaunchedEffect(uiState.error) {
            guideDetailsViewModel.dismissAlert()
        }
        ErrorAlertDialog(uiState.error)
    }
}

@Composable
private fun GuideHeroCard(guide: Guide) {
    val completedSections = guide.sections.count { it.completed }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = guide.itemName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.Bold,
            )

            if (guide.description.isNotBlank()) {
                Text(
                    text = guide.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.background,
                )
            }

            Text(
                text = buildString {
                    append(if (guide.visibility == GuideVisibility.FAMILY) "Family shared" else "Private")
                    append("  •  ")
                    append("Sections: $completedSections/${guide.sections.size}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.background,
            )

            Text(
                text = if (guide.started) "Started" else "Not started",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun GuideSectionCard(
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

@Composable
private fun GuideStepRows(
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

@Composable
private fun StepToggleRow(
    text: String,
    isCompleted: Boolean,
    enabled: Boolean,
    textColor: Color,
    indentLevel: Int,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 18).dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.alpha(if (enabled) 1f else 0.45f)) {
            CompletableBox(
                isCompleted = isCompleted,
                onCompleteToggle = {
                    if (enabled) {
                        onToggle()
                    }
                },
            )
        }

        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

@Composable
private fun CommentBubble(
    comment: String,
    textColor: Color,
    surfaceColor: Color,
    label: String,
    indentLevel: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ((indentLevel * 18) + 40).dp)
            .background(surfaceColor, RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = comment,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
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

private fun commentText(step: GuideStep): String {
    if (step.content.isNotBlank()) return step.content
    if (step.name.isNotBlank()) return step.name
    if (step.title.isNotBlank()) return step.title
    return "Comment"
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
