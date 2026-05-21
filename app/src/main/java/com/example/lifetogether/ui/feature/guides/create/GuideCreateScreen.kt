package com.example.lifetogether.ui.feature.guides.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun GuideCreateScreen(
    uiState: GuideCreateUiState,
    onUiEvent: (GuideCreateUiEvent) -> Unit,
    onNavigationEvent: (GuideCreateNavigationEvent) -> Unit,
) {
    var visibilityExpanded by remember { mutableStateOf(false) }
    var newSectionTitle by remember { mutableStateOf("") }
    var newSectionAmount by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(GuideCreateNavigationEvent.NavigateBack) },
                text = "Create guide",
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(LifeTogetherTokens.spacing.medium),
            ) {
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Save guide",
                    onClick = { onUiEvent(GuideCreateUiEvent.SaveClicked) },
                    loading = uiState.isSaving,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LifeTogetherTokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            item {
                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.title,
                    onValueChange = { onUiEvent(GuideCreateUiEvent.TitleChanged(it)) },
                    label = "Title",
                    capitalization = true,
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text,
                )
            }

            item {
                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.description,
                    onValueChange = { onUiEvent(GuideCreateUiEvent.DescriptionChanged(it)) },
                    label = "Description",
                    capitalization = true,
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text,
                )
            }

            item {
                Dropdown(
                    selectedValue = if (uiState.visibility == Visibility.FAMILY) "Family shared" else "Private",
                    expanded = visibilityExpanded,
                    onExpandedChange = { visibilityExpanded = it },
                    options = listOf("Private", "Family shared"),
                    label = "Visibility",
                    onValueChangedEvent = {
                        onUiEvent(
                            GuideCreateUiEvent.VisibilityChanged(
                                if (it == "Family shared") Visibility.FAMILY else Visibility.PRIVATE,
                            ),
                        )
                    },
                )
            }

            // ─── Add section form ──────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                ) {
                    TextDefault(text = "Sections (optional)")
                    CustomTextField(
                        value = newSectionTitle,
                        onValueChange = { newSectionTitle = it },
                        label = "Section title",
                        capitalization = true,
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CustomTextField(
                            modifier = Modifier.weight(0.6f),
                            value = newSectionAmount,
                            onValueChange = { newSectionAmount = it.filter { c -> c.isDigit() } },
                            label = "Repetitions",
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Number,
                        )
                        PrimaryButton(
                            text = "Add",
                            onClick = {
                                val amount = newSectionAmount.toIntOrNull() ?: 1
                                onUiEvent(GuideCreateUiEvent.AddSectionRequested(newSectionTitle, amount))
                                newSectionTitle = ""
                                newSectionAmount = "1"
                            },
                        )
                    }
                    Text(
                        text = "Repetitions: how many times to run through this section",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
            }

            // ─── Sections list ────────────────────────────────────────────────
            if (uiState.sections.isNotEmpty()) {
                item {
                    ReorderableColumn(
                        modifier = Modifier.fillMaxWidth(),
                        list = uiState.sections,
                        onSettle = { from, to ->
                            onUiEvent(GuideCreateUiEvent.SectionMoved(from, to))
                        },
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                    ) { _, section, _ ->
                        key(section.id) {
                            ReorderableItem {
                                val sectionDragModifier = Modifier.longPressDraggableHandle()
                                SectionCard(
                                    section = section,
                                    stepDraft = uiState.stepDrafts[section.id] ?: "",
                                    stepTypeDraft = uiState.stepTypeDrafts[section.id]
                                        ?: GuideStepType.NUMBERED,
                                    sectionDragModifier = sectionDragModifier,
                                    onStepDraftChange = {
                                        onUiEvent(GuideCreateUiEvent.StepDraftChanged(section.id, it))
                                    },
                                    onStepTypeDraftChange = {
                                        onUiEvent(GuideCreateUiEvent.StepTypeDraftChanged(section.id, it))
                                    },
                                    onAddStep = { content, type ->
                                        onUiEvent(
                                            GuideCreateUiEvent.AddStepRequested(section.id, content, type),
                                        )
                                    },
                                    onDeleteSection = {
                                        onUiEvent(GuideCreateUiEvent.DeleteSectionRequested(section.id))
                                    },
                                    onDeleteStep = { stepId ->
                                        onUiEvent(
                                            GuideCreateUiEvent.DeleteStepRequested(section.id, stepId),
                                        )
                                    },
                                    onStepMoved = { from, to ->
                                        onUiEvent(GuideCreateUiEvent.StepMoved(section.id, from, to))
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.padding(LifeTogetherTokens.spacing.small)) }
        }
    }
}

@Composable
private fun SectionCard(
    section: GuideSection,
    stepDraft: String,
    stepTypeDraft: GuideStepType,
    sectionDragModifier: Modifier,
    onStepDraftChange: (String) -> Unit,
    onStepTypeDraftChange: (GuideStepType) -> Unit,
    onAddStep: (content: String, type: GuideStepType) -> Unit,
    onDeleteSection: () -> Unit,
    onDeleteStep: (stepId: String) -> Unit,
    onStepMoved: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    var stepTypeExpanded by remember(section.id) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {

            // Section header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_drag_handle),
                    contentDescription = "Drag to reorder section",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = sectionDragModifier.size(LifeTogetherTokens.sizing.iconLarge),
                )
                Text(
                    text = buildString {
                        append(section.title)
                        if (section.amount > 1) append(" (×${section.amount})")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_trashcan),
                    contentDescription = "Delete section",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .size(LifeTogetherTokens.sizing.iconMedium)
                        .clickable { onDeleteSection() },
                )
            }

            // Steps list
            if (section.steps.isNotEmpty()) {
                ReorderableColumn(
                    modifier = Modifier.fillMaxWidth(),
                    list = section.steps,
                    onSettle = { from, to -> onStepMoved(from, to) },
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                ) { _, step, _ ->
                    key(step.id) {
                        ReorderableItem {
                            val stepDragModifier = Modifier.longPressDraggableHandle()
                            StepRow(
                                step = step,
                                dragModifier = stepDragModifier,
                                onDelete = { onDeleteStep(step.id) },
                            )
                        }
                    }
                }
            }

            // Step draft field
            CustomTextField(
                modifier = Modifier.fillMaxWidth(),
                value = stepDraft,
                onValueChange = onStepDraftChange,
                label = stepFieldLabel(stepTypeDraft),
                capitalization = true,
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
                keyboardActions = KeyboardActions(
                    onDone = { onAddStep(stepDraft, stepTypeDraft) },
                ),
            )

            // Round type hint
            if (stepTypeDraft == GuideStepType.ROUND) {
                Text(
                    text = "Type \"R1-3 content\" to create steps for rounds 1 to 3",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }

            // Type selector + Add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dropdown(
                    modifier = Modifier.weight(1f),
                    selectedValue = toStepTypeLabel(stepTypeDraft),
                    expanded = stepTypeExpanded,
                    onExpandedChange = { stepTypeExpanded = it },
                    options = listOf("Numbered", "Round", "Comment", "Subsection"),
                    label = "Step type",
                    onValueChangedEvent = { onStepTypeDraftChange(fromStepTypeLabel(it)) },
                )
                PrimaryButton(
                    text = "Add",
                    onClick = { onAddStep(stepDraft, stepTypeDraft) },
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    step: GuideStep,
    dragModifier: Modifier,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = "Drag to reorder step",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = dragModifier.size(LifeTogetherTokens.sizing.iconLarge),
        )
        TextLabel(text = toStepTypeLabel(step.type))
        Text(
            text = stepDisplayText(step),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(R.drawable.ic_trashcan),
            contentDescription = "Delete step",
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(LifeTogetherTokens.sizing.iconMedium)
                .clickable { onDelete() },
        )
    }
}

private fun stepDisplayText(step: GuideStep): String = when (step.type) {
    GuideStepType.ROUND -> if (step.name.isNotBlank()) "${step.name}: ${step.content}" else step.content
    GuideStepType.SUBSECTION -> step.title.ifBlank { step.content }
    else -> step.content
}

private fun stepFieldLabel(type: GuideStepType): String = when (type) {
    GuideStepType.SUBSECTION -> "Subsection title"
    GuideStepType.COMMENT -> "Comment"
    else -> "Step content"
}

private fun toStepTypeLabel(type: GuideStepType): String = when (type) {
    GuideStepType.NUMBERED -> "Numbered"
    GuideStepType.ROUND -> "Round"
    GuideStepType.COMMENT -> "Comment"
    GuideStepType.SUBSECTION -> "Subsection"
    GuideStepType.UNKNOWN -> "Comment"
}

private fun fromStepTypeLabel(label: String): GuideStepType = when (label) {
    "Round" -> GuideStepType.ROUND
    "Comment" -> GuideStepType.COMMENT
    "Subsection" -> GuideStepType.SUBSECTION
    else -> GuideStepType.NUMBERED
}

@Preview(showBackground = true)
@Composable
private fun GuideCreateScreenPreview() {
    LifeTogetherTheme {
        GuideCreateScreen(
            uiState = GuideCreateUiState(
                title = "Family reset",
                description = "A simple weekly reset guide",
                visibility = Visibility.PRIVATE,
                sections = listOf(
                    GuideSection(
                        id = "section-1",
                        orderNumber = 1,
                        title = "Intro",
                        amount = 1,
                        steps = emptyList(),
                    ),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
