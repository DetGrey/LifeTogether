package com.example.lifetogether.ui.feature.guides.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon as AppIcon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.feature.guides.details.components.GuideHeroCard
import com.example.lifetogether.ui.feature.guides.details.components.GuideSectionCard
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun GuideDetailsScreen(
    uiState: GuideDetailsUiState,
    onUiEvent: (GuideDetailsUiEvent) -> Unit,
    onNavigationEvent: (GuideDetailsNavigationEvent) -> Unit,
) {
    val guide = (uiState as? GuideDetailsUiState.Content)?.guide
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetProgressDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                leftIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(GuideDetailsNavigationEvent.NavigateBack)
                },
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
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is GuideDetailsUiState.Loading,
            label = "guide_details_loading_content",
            loadingContent = {
                Skeletons.SectionDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = uiState as? GuideDetailsUiState.Content ?: return@AnimatedLoadingContent
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LifeTogetherTokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            ) {
                val guide = content.guide
                if (guide == null) {
                    item {
                        Skeletons.SectionDetail(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                } else {
                    item {
                        GuideHeroCard(guide)
                    }

                    item {
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = if (guide.started) "Continue where you left off" else "Start guide",
                            loading = content.isStartingGuide,
                            onClick = { onUiEvent(GuideDetailsUiEvent.StartOrContinueClicked) },
                        )
                    }

                    if (guide.sections.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    .padding(LifeTogetherTokens.spacing.medium),
                            ) {
                                TextDefault(
                                    text = "No sections yet",
                                )
                            }
                        }
                    } else {
                        guide.sections.forEachIndexed { sectionIndex, section ->
                            val sectionKey = guideSectionKey(section, sectionIndex)
                            val isExpanded = content.sectionExpandedState[sectionKey] ?: true
                            val selectedAmountIndex =
                                content.selectedSectionAmountState[sectionKey]
                                    ?: defaultSectionAmountIndex(section)

                            item(key = sectionKey) {
                                GuideSectionCard(
                                    section = section,
                                    selectedAmountIndex = selectedAmountIndex,
                                    onSelectAmountIndex = { amountIndex ->
                                        onUiEvent(
                                            GuideDetailsUiEvent.SelectSectionAmount(
                                                sectionKey = sectionKey,
                                                amountIndex = amountIndex,
                                            ),
                                        )
                                    },
                                    expanded = isExpanded,
                                    onToggleExpanded = {
                                        onUiEvent(
                                            GuideDetailsUiEvent.ToggleSectionExpanded(
                                                sectionKey,
                                            )
                                        )
                                    },
                                    canToggleStep = { amountIndex ->
                                        content.canToggleAmountState[sectionKey]
                                            ?.contains(amountIndex) == true
                                    },
                                    onToggleStep = { amountIndex, stepId ->
                                        onUiEvent(
                                            GuideDetailsUiEvent.ToggleStepCompletion(
                                                stepId = stepId,
                                                amountIndex = amountIndex,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOverflowMenu && guide != null) {
        val visibilityActionLabel = if (guide.visibility == Visibility.FAMILY) {
            "Make private"
        } else {
            "Share with family"
        }

        ActionSheet(
            onDismiss = { showOverflowMenu = false },
            actionsList = listOf(
                ActionSheetItem(
                    label = "Reset all progress",
                    onClick = {
                        showOverflowMenu = false
                        showResetProgressDialog = true
                    },
                    isDestructive = true,
                ),
                ActionSheetItem(
                    label = visibilityActionLabel,
                    onClick = {
                        showOverflowMenu = false
                        onUiEvent(GuideDetailsUiEvent.ToggleVisibilityClicked)
                    },
                ),
                ActionSheetItem(
                    label = "Delete guide",
                    onClick = {
                        showOverflowMenu = false
                        showDeleteDialog = true
                    },
                    isDestructive = true,
                ),
            ),
        )
    }

    if (showResetProgressDialog) {
        ConfirmationDialog(
            onDismiss = { showResetProgressDialog = false },
            onConfirm = {
                showResetProgressDialog = false
                onUiEvent(GuideDetailsUiEvent.ResetAllProgressClicked)
            },
            dialogTitle = "Reset all progress",
            dialogMessage = "Are you sure you want to reset all progress for this guide?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Reset progress",
        )
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onUiEvent(GuideDetailsUiEvent.DeleteGuideClicked)
            },
            dialogTitle = "Delete guide",
            dialogMessage = "Are you sure you want to delete this guide?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete guide",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideDetailsScreenPreview() {
    LifeTogetherTheme {
        GuideDetailsScreen(
            uiState = GuideDetailsUiState.Content(
                guide = Guide(
                    itemName = "Family reset",
                    description = "A simple weekly reset guide",
                    visibility = Visibility.FAMILY,
                    started = true,
                    sections = emptyList(),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GuideDetailsScreenLoadingPreview() {
    LifeTogetherTheme {
        GuideDetailsScreen(
            uiState = GuideDetailsUiState.Loading,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
