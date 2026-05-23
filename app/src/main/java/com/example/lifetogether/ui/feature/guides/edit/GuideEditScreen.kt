package com.example.lifetogether.ui.feature.guides.edit

import androidx.activity.compose.BackHandler
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
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun GuideEditScreen(
    uiState: GuideEditUiState,
    onUiEvent: (GuideEditUiEvent) -> Unit,
) {
    val content = uiState as? GuideEditUiState.Content
    var showDiscardDialog by remember { mutableStateOf(false) }
    var visibilityExpanded by remember { mutableStateOf(false) }
    var newSectionTitle by remember { mutableStateOf("") }
    var newSectionPieces by remember { mutableStateOf("1") }

    BackHandler {
        if (content != null) showDiscardDialog = true else onUiEvent(GuideEditUiEvent.DiscardClicked)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    if (content != null) showDiscardDialog = true else onUiEvent(GuideEditUiEvent.DiscardClicked)
                },
                text = if (content?.isEditMode == true) "Edit guide" else "Create guide",
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
                    onClick = { onUiEvent(GuideEditUiEvent.SaveClicked) },
                    loading = content?.isSaving == true,
                )
            }
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is GuideEditUiState.Loading,
            label = "edit_guide_loading_content",
            loadingContent = {
                Skeletons.FormEdit(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = uiState as? GuideEditUiState.Content ?: return@AnimatedLoadingContent
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
                        value = content.title,
                        onValueChange = { onUiEvent(GuideEditUiEvent.TitleChanged(it)) },
                        label = "Title",
                        capitalization = true,
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text,
                    )
                }

                item {
                    CustomTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = content.description,
                        onValueChange = { onUiEvent(GuideEditUiEvent.DescriptionChanged(it)) },
                        label = "Description",
                        capitalization = true,
                        imeAction = ImeAction.Next,
                        keyboardType = KeyboardType.Text,
                    )
                }

                item {
                    Dropdown(
                        selectedValue = if (content.visibility == Visibility.FAMILY) "Family shared" else "Private",
                        expanded = visibilityExpanded,
                        onExpandedChange = { visibilityExpanded = it },
                        options = listOf("Private", "Family shared"),
                        label = "Visibility",
                        onValueChangedEvent = {
                            onUiEvent(
                                GuideEditUiEvent.VisibilityChanged(
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
                                value = newSectionPieces,
                                onValueChange = {
                                    newSectionPieces = it.filter { ch -> ch.isDigit() }
                                },
                                label = "Pieces",
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Number,
                            )
                            PrimaryButton(
                                text = "Add",
                                onClick = {
                                    val pieces = newSectionPieces.toIntOrNull() ?: 1
                                    onUiEvent(
                                        GuideEditUiEvent.AddSectionRequested(
                                            newSectionTitle,
                                            pieces
                                        )
                                    )
                                    newSectionTitle = ""
                                    newSectionPieces = "1"
                                },
                            )
                        }
                        Text(
                            text = "How many identical pieces to make (e.g. 2 legs, 2 ears)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }

                // ─── Sections list ────────────────────────────────────────────────
                if (content.sections.isNotEmpty()) {
                    item {
                        ReorderableColumn(
                            modifier = Modifier.fillMaxWidth(),
                            list = content.sections,
                            onSettle = { from, to ->
                                onUiEvent(GuideEditUiEvent.SectionMoved(from, to))
                            },
                            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                        ) { _, section, _ ->
                            key(section.id) {
                                ReorderableItem {
                                    val sectionDragModifier = Modifier.longPressDraggableHandle()
                                    SectionCard(
                                        section = section,
                                        stepDraft = content.stepDrafts[section.id] ?: "",
                                        stepTypeDraft = content.stepTypeDrafts[section.id]
                                            ?: GuideStepType.NUMBERED,
                                        sectionDragModifier = sectionDragModifier,
                                        onStepDraftChange = {
                                            onUiEvent(
                                                GuideEditUiEvent.StepDraftChanged(
                                                    section.id,
                                                    it
                                                )
                                            )
                                        },
                                        onStepTypeDraftChange = {
                                            onUiEvent(
                                                GuideEditUiEvent.StepTypeDraftChanged(
                                                    section.id,
                                                    it
                                                )
                                            )
                                        },
                                        onAddStep = { stepContent, type ->
                                            onUiEvent(
                                                GuideEditUiEvent.AddStepRequested(
                                                    section.id,
                                                    stepContent,
                                                    type
                                                ),
                                            )
                                        },
                                        onDeleteSection = {
                                            onUiEvent(
                                                GuideEditUiEvent.DeleteSectionRequested(
                                                    section.id
                                                )
                                            )
                                        },
                                        onDeleteStep = { stepId ->
                                            onUiEvent(
                                                GuideEditUiEvent.DeleteStepRequested(
                                                    section.id,
                                                    stepId
                                                ),
                                            )
                                        },
                                        onStepMoved = { from, to ->
                                            onUiEvent(
                                                GuideEditUiEvent.StepMoved(
                                                    section.id,
                                                    from,
                                                    to
                                                )
                                            )
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

    if (showDiscardDialog) {
        ConfirmationDialog(
            onDismiss = { showDiscardDialog = false },
            onConfirm = {
                showDiscardDialog = false
                onUiEvent(GuideEditUiEvent.ConfirmDiscard)
            },
            dialogTitle = "Discard changes?",
            dialogMessage = "Your unsaved changes will be lost.",
            dismissButtonMessage = "Keep editing",
            confirmButtonMessage = "Discard",
        )
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
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = sectionDragModifier.size(LifeTogetherTokens.sizing.iconLarge),
                )
                TextHeadingMedium(
                    text = buildString {
                        append(section.title)
                        if (section.pieces > 1) append(" (×${section.pieces})")
                    },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Delete section",
                    tint = MaterialTheme.colorScheme.error,
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
            painter = painterResource(R.drawable.ic_delete),
            contentDescription = "Delete step",
            tint = MaterialTheme.colorScheme.error,
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
private fun GuideEditScreenPreview() {
    LifeTogetherTheme {
        GuideEditScreen(
            uiState = GuideEditUiState.Content(
                title = "Family reset",
                description = "A simple weekly reset guide",
                visibility = Visibility.PRIVATE,
                sections = listOf(
                    GuideSection(
                        id = "section-1",
                        orderNumber = 1,
                        title = "Intro",
                        pieces = 1,
                        steps = emptyList(),
                    ),
                ),
            ),
            onUiEvent = {},
        )
    }
}
