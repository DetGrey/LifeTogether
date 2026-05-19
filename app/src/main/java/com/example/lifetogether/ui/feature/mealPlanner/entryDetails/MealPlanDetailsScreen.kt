package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun MealPlanDetailsScreen(
    uiState: MealPlanDetailsUiState,
    mealPlanId: String? = null,
    familyId: String? = null,
    onUiEvent: (MealPlanDetailsUiEvent) -> Unit,
    onNavigationEvent: (MealPlanDetailsNavigationEvent) -> Unit,
) {
    val content = uiState as? MealPlanDetailsUiState.Content
    val isLoading = uiState is MealPlanDetailsUiState.Loading
    val isExistingMealPlan = mealPlanId != null

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                onLeftClick = {
                    if (content?.isEditing == true) {
                        onUiEvent(MealPlanDetailsUiEvent.RequestCancelEdit)
                    } else {
                        onNavigationEvent(MealPlanDetailsNavigationEvent.NavigateBack)
                    }
                },
                text = "Meal Plan",
                rightAppIcon = when {
                    !isExistingMealPlan -> null
                    content?.isEditing == true -> AppIcon(resId = R.drawable.ic_trashcan, description = "delete meal plan entry")
                    else -> AppIcon(resId = R.drawable.ic_edit, description = "edit meal plan entry")
                },
                onRightClick = when {
                    isExistingMealPlan && content?.isEditing == true -> { { onUiEvent(MealPlanDetailsUiEvent.RequestDeleteMealPlan) } }
                    isExistingMealPlan -> { { onUiEvent(MealPlanDetailsUiEvent.EnterEditMode) } }
                    else -> null
                },
            )
        }
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = isLoading,
            label = "meal_planner_details_loading",
            loadingContent = {
                Skeletons.FormEdit(modifier = Modifier.fillMaxSize())
            },
        ) {
            val contentState = content ?: return@AnimatedLoadingContent
            val details = contentState.details as? MealPlanDetailsContent.Meal ?: return@AnimatedLoadingContent
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .padding(LifeTogetherTokens.spacing.small),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(
                        LifeTogetherTokens.spacing.large,
                    ),
                ) {
                    mealPlanContent(
                        uiState = contentState,
                        familyId = familyId,
                        formState = details.form,
                        searchState = contentState.mealRecipeSearchState,
                        onUiEvent = onUiEvent,
                        onNavigationEvent = onNavigationEvent,
                    )
                }
                AnimatedVisibility(
                    visible = contentState.isEditing,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .height(50.dp)
                            .padding(bottom = padding.calculateBottomPadding())
                            .align(Alignment.End),
                    ) {
                        PrimaryButton(
                            modifier = Modifier.fillMaxWidth(),
                            text = if (isExistingMealPlan) "Save changes" else "Create",
                            onClick = { onUiEvent(MealPlanDetailsUiEvent.SaveClicked) },
                            loading = contentState.isSaving,
                        )
                    }
                }
            }
        }
    }

    if (content?.showDiscardDialog == true) {
        ConfirmationDialog(
            onDismiss = { onUiEvent(MealPlanDetailsUiEvent.DismissDiscardDialog) },
            onConfirm = { onUiEvent(MealPlanDetailsUiEvent.ConfirmDiscard) },
            dialogTitle = "Discard changes?",
            dialogMessage = "Your unsaved changes will be lost.",
            dismissButtonMessage = "Keep editing",
            confirmButtonMessage = "Discard",
        )
    }

    if (content?.showDeleteDialog == true) {
        ConfirmationDialog(
            onDismiss = { onUiEvent(MealPlanDetailsUiEvent.DismissDeleteDialog) },
            onConfirm = { onUiEvent(MealPlanDetailsUiEvent.ConfirmDeleteMealPlan) },
            dialogTitle = "Delete meal plan entry?",
            dialogMessage = "This will permanently delete the meal plan entry.",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MealPlanDetailsScreenPreview() {
    LifeTogetherTheme {
    }
}
