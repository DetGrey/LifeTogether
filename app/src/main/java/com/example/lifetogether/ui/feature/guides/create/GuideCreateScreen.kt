package com.example.lifetogether.ui.feature.guides.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun GuideCreateScreen(
    uiState: GuideCreateUiState,
    onUiEvent: (GuideCreateUiEvent) -> Unit,
    onNavigationEvent: (GuideCreateNavigationEvent) -> Unit,
) {
    var visibilityExpanded by remember { mutableStateOf(false) }
    var newSectionTitle by remember { mutableStateOf("") }
    var newSectionAmount by remember { mutableStateOf("1") }
    val stepDrafts = remember { mutableStateMapOf<String, String>() }
    val stepTypeDrafts = remember { mutableStateMapOf<String, GuideStepType>() }

    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(GuideCreateNavigationEvent.NavigateBack) },
                text = "Create guide",
            )
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
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                )
            }

            item {
                CustomTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = uiState.description,
                    onValueChange = { onUiEvent(GuideCreateUiEvent.DescriptionChanged(it)) },
                    label = "Description",
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text
                )
            }

            item {
                Dropdown(
                    selectedValue = if (uiState.visibility == Visibility.FAMILY) {
                        "Family shared"
                    } else {
                        "Private"
                    },
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

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    TextDefault(
                        text = "Sections (optional)",
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CustomTextField(
                            modifier = Modifier.weight(1f),
                            value = newSectionTitle,
                            onValueChange = { newSectionTitle = it },
                            label = "Section title",
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        )
                        CustomTextField(
                            modifier = Modifier.weight(0.5f),
                            value = newSectionAmount,
                            onValueChange = { newSectionAmount = it.filter { char -> char.isDigit() } },
                            label = "Amount",
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
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
                }
            }

            uiState.sections.forEach { section ->
                item(key = section.id) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                            .padding(LifeTogetherTokens.spacing.medium),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {
                            Text(
                                text = buildString {
                                    append(section.title)
                                    if (section.amount > 1) {
                                        append(" (x${section.amount})")
                                    }
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )

                            var numberedIndex = 1
                            section.steps.forEach { step ->
                                val stepText = when (step.type) {
                                    GuideStepType.ROUND -> "${step.name.ifBlank { step.title.ifBlank { "Round" } }} ${step.content}".trim()
                                    GuideStepType.COMMENT -> step.content
                                    GuideStepType.NUMBERED -> {
                                        val text = "${numberedIndex}. ${step.content}"
                                        numberedIndex += 1
                                        text
                                    }
                                    GuideStepType.SUBSECTION -> "Subsection: ${step.title.ifBlank { step.name }}"
                                    GuideStepType.UNKNOWN -> step.content
                                }
                                TextDefault(
                                    text = stepText,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }

                            CustomTextField(
                                modifier = Modifier.fillMaxWidth(),
                                value = stepDrafts[section.id] ?: "",
                                onValueChange = { stepDrafts[section.id] = it },
                                label = "Step name",
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Text
                            )

                            var stepTypeExpanded by remember(section.id) { mutableStateOf(false) }
                            Dropdown(
                                selectedValue = toStepTypeLabel(stepTypeDrafts[section.id] ?: GuideStepType.NUMBERED),
                                expanded = stepTypeExpanded,
                                onExpandedChange = { stepTypeExpanded = it },
                                options = listOf("Numbered", "Round", "Comment", "Subsection"),
                                label = "Step type",
                                onValueChangedEvent = { selectedLabel ->
                                    stepTypeDrafts[section.id] = fromStepTypeLabel(selectedLabel)
                                },
                            )

                            PrimaryButton(
                                text = "Add step",
                                onClick = {
                                    val draftText = stepDrafts[section.id].orEmpty()
                                    onUiEvent(
                                        GuideCreateUiEvent.AddStepRequested(
                                            sectionId = section.id,
                                            content = draftText,
                                            type = stepTypeDrafts[section.id] ?: GuideStepType.NUMBERED,
                                        ),
                                    )
                                    stepDrafts[section.id] = ""
                                },
                            )
                        }
                    }
                }
            }

            item {
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = "Save guide",
                    onClick = { onUiEvent(GuideCreateUiEvent.SaveClicked) },
                    loading = uiState.isSaving,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideCreateScreenPreview() {
    LifeTogetherTheme {
        GuideCreateScreen(
            uiState = GuideCreateUiState(
                title = "Family reset",
                description = "A simple weekly reset guide",
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

private fun toStepTypeLabel(type: GuideStepType): String {
    return when (type) {
        GuideStepType.NUMBERED -> "Numbered"
        GuideStepType.ROUND -> "Round"
        GuideStepType.COMMENT -> "Comment"
        GuideStepType.SUBSECTION -> "Subsection"
        GuideStepType.UNKNOWN -> "Comment"
    }
}

private fun fromStepTypeLabel(label: String): GuideStepType {
    return when (label) {
        "Round" -> GuideStepType.ROUND
        "Comment" -> GuideStepType.COMMENT
        "Subsection" -> GuideStepType.SUBSECTION
        else -> GuideStepType.NUMBERED
    }
}
