package com.example.lifetogether.ui.feature.guides.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon as AppIcon
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.ui.common.OverflowMenu
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.feature.guides.details.components.GuideHeroCard
import com.example.lifetogether.ui.feature.guides.details.components.GuideSectionCard
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.navigation.GuideStepPlayerNavRoute

@Composable
fun GuideDetailsScreen(
    appNavigator: AppNavigator? = null,
    guideId: String,
    guideDetailsViewModel: GuideDetailsViewModel,
) {
    val uiState by guideDetailsViewModel.uiState.collectAsState()
    val guide = uiState.guide

    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetProgressDialog by remember { mutableStateOf(false) }

    LaunchedEffect(guideId) {
        guideDetailsViewModel.setUp(guideId) //todo don't think this is the best way
    }

    DisposableEffect(Unit) {
        onDispose {
            guideDetailsViewModel.flushPendingChanges()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            TopBar(
                leftIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    guideDetailsViewModel.flushPendingChanges()
                    appNavigator?.navigateBack()
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
                            appNavigator?.navigate(GuideStepPlayerNavRoute(guideId))
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
                    val sectionKey = guideSectionKey(section, sectionIndex)
                    val isExpanded = uiState.sectionExpandedState[sectionKey] ?: true
                    val selectedAmountIndex = uiState.selectedSectionAmountState[sectionKey]
                        ?: defaultSectionAmountIndex(section)

                    item(key = sectionKey) {
                        GuideSectionCard(
                            section = section,
                            selectedAmountIndex = selectedAmountIndex,
                            onSelectAmountIndex = { amountIndex ->
                                guideDetailsViewModel.selectSectionAmount(
                                    sectionKey = sectionKey,
                                    amountIndex = amountIndex,
                                )
                            },
                            expanded = isExpanded,
                            onToggleExpanded = {
                                guideDetailsViewModel.toggleSectionExpanded(sectionKey)
                            },
                            canToggleStep = { amountIndex, stepId ->
                                guideDetailsViewModel.canToggleStep(
                                    stepId = stepId,
                                    amountIndex = amountIndex,
                                )
                            },
                            onToggleStep = { amountIndex, stepId ->
                                guideDetailsViewModel.toggleStepCompletion(
                                    stepId = stepId,
                                    amountIndex = amountIndex,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (showOverflowMenu && guide != null) {
        val canModifyGuide = guideDetailsViewModel.canToggleVisibility()
        val visibilityActionLabel = if (guide.visibility == Visibility.FAMILY) {
            "Make private"
        } else {
            "Share with family"
        }

        OverflowMenu(
            onDismiss = { showOverflowMenu = false },
            actionsList = listOf(
                mapOf("Reset all progress" to {
                    showOverflowMenu = false
                    showResetProgressDialog = true
                }),
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

    if (showResetProgressDialog) {
        ConfirmationDialog(
            onDismiss = { showResetProgressDialog = false },
            onConfirm = {
                showResetProgressDialog = false
                guideDetailsViewModel.resetAllProgress()
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
