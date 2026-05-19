package com.example.lifetogether.ui.feature.recipes

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.add.AddNewString
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.image.EditableImageCard
import com.example.lifetogether.ui.common.list.CompletableCategoryList
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.tagOptionRow.TagOption
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.textfield.EditableTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

private val recipeServingsOptions = (1..100).map(Int::toString)

@Composable
fun RecipeDetailsScreen(
    uiState: RecipeDetailsUiState,
    bitmap: Bitmap?,
    onUiEvent: (RecipeDetailsUiEvent) -> Unit,
    onNavigationEvent: (RecipeDetailsNavigationEvent) -> Unit,
) {
    val contentState = uiState as? RecipeDetailsUiState.Content
    val hasRecipeId = contentState?.recipeId != null
    val showDeleteAction = hasRecipeId && contentState.editMode
    val showEditAction = hasRecipeId && !contentState.editMode

    BackHandler(enabled = contentState?.editMode == true) {
        onUiEvent(RecipeDetailsUiEvent.DiscardClicked)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    if (contentState?.editMode == true) {
                        onUiEvent(RecipeDetailsUiEvent.DiscardClicked)
                    } else {
                        onNavigationEvent(RecipeDetailsNavigationEvent.NavigateBack)
                    }
                },
                text = "",
                rightAppIcon = if (showDeleteAction) {
                    AppIcon(
                        resId = R.drawable.ic_trashcan,
                        description = "trashcan icon",
                    )
                } else if (showEditAction) {
                    AppIcon(
                        resId = R.drawable.ic_edit,
                        description = "edit icon",
                    )
                } else { null },
                onRightClick = if (showDeleteAction) {
                    { onUiEvent(RecipeDetailsUiEvent.DeleteClicked) }
                } else if (showEditAction) {
                    { onUiEvent(RecipeDetailsUiEvent.EditClicked) }
                } else { null },
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is RecipeDetailsUiState.Loading,
            label = "recipe_details_loading_content",
            loadingContent = {
                Skeletons.FormEdit(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = contentState ?: return@AnimatedLoadingContent
            RecipeDetailsContent(
                uiState = content,
                bitmap = bitmap,
                modifier = Modifier
                    .padding(padding)
                    .padding(horizontal = LifeTogetherTokens.spacing.medium),
                onUiEvent = onUiEvent,
            )
        }
    }
}

@Composable
private fun RecipeDetailsContent(
    uiState: RecipeDetailsUiState.Content,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    onUiEvent: (RecipeDetailsUiEvent) -> Unit,
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            onUiEvent(RecipeDetailsUiEvent.RecipeImageSelected(it))
        }
    }

    val displayedBitmap = uiState.localImageBitmap ?: bitmap
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.large),
    ) {
        item {
            Box(
                modifier = Modifier
                    .padding(
                        start = LifeTogetherTokens.spacing.small,
                        end = LifeTogetherTokens.spacing.medium,
                    ),
            ) {
                EditableTextField(
                    text = uiState.itemName,
                    onTextChange = { onUiEvent(RecipeDetailsUiEvent.ItemNameChanged(it)) },
                    label = "Recipe name",
                    isEditable = uiState.editMode,
                    textStyle = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    showLabelAsPlaceholder = true,
                    hideBackground = true,
                )
            }

            EditableImageCard(
                bitmap = displayedBitmap,
                isEditing = uiState.editMode,
                onLaunchImagePicker = { imagePickerLauncher.launch(it) },
            )
        }

        item {
            if (uiState.editMode || uiState.description.isNotBlank()) {
                EditableTextField(
                    text = uiState.description,
                    onTextChange = { onUiEvent(RecipeDetailsUiEvent.DescriptionChanged(it)) },
                    label = "Description",
                    isEditable = uiState.editMode,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    showLabelAsPlaceholder = true,
                )
            }
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextDefault("Preparation time in minutes: ")
                    EditableTextField(
                        text = uiState.preparationTimeMin,
                        onTextChange = {
                            onUiEvent(RecipeDetailsUiEvent.PreparationTimeChanged(it))
                        },
                        label = "E.g. 30",
                        isEditable = uiState.editMode,
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                        showLabelAsPlaceholder = true,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth(if (uiState.editMode) 1f else 0.45f)
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextDefault("Servings: ")
                    if (uiState.editMode) {
                        EditableTextField(
                            text = uiState.servings,
                            onTextChange = {
                                onUiEvent(RecipeDetailsUiEvent.ServingsChanged(it))
                            },
                            label = "E.g. 2",
                            isEditable = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            showLabelAsPlaceholder = true,
                        )
                    } else {
                        Dropdown(
                            selectedValue = uiState.servings,
                            expanded = uiState.servingsExpanded,
                            onExpandedChange = {
                                onUiEvent(RecipeDetailsUiEvent.ServingsExpandedChanged(it))
                            },
                            options = recipeServingsOptions,
                            label = null,
                            onValueChangedEvent = {
                                onUiEvent(RecipeDetailsUiEvent.ServingsChanged(it))
                            },
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                ) {
                    TextDefault("Tags:")
                    if (uiState.editMode) {
                        EditableTextField(
                            text = uiState.tagsInput,
                            onTextChange = {
                                onUiEvent(RecipeDetailsUiEvent.TagsChanged(it))
                            },
                            label = "E.g. \"dinner pasta\"",
                            isEditable = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                            capitalization = false,
                            showLabelAsPlaceholder = true,
                        )
                    } else {
                        for (tag in uiState.tags) {
                            TagOption(
                                tag = tag,
                                selectedTag = tag,
                            )
                        }
                    }
                }
            }
        }

        item {
            uiState.expandedStates["ingredients"]?.let { expanded ->
                CompletableCategoryList(
                    category = Category(
                        "🍎",
                        "Ingredients",
                    ),
                    itemList = if (uiState.editMode) uiState.ingredients else uiState.ingredientsByServings,
                    expanded = expanded,
                    onClick = {
                        onUiEvent(RecipeDetailsUiEvent.ToggleIngredientsExpanded)
                    },
                    onCompleteToggle = {
                        onUiEvent(RecipeDetailsUiEvent.IngredientCompletedToggled(it))
                    },
                )
            }

            AnimatedVisibility(
                visible = uiState.editMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                AddNewIngredient(
                    onAddClick = {
                        onUiEvent(RecipeDetailsUiEvent.AddIngredientClicked(it))
                    },
                )
            }
        }

        item {
            uiState.expandedStates["instructions"]?.let { expanded ->
                CompletableCategoryList(
                    category = Category(
                        "✔️",
                        "Instructions",
                    ),
                    itemList = uiState.instructions,
                    expanded = expanded,
                    onClick = {
                        onUiEvent(RecipeDetailsUiEvent.ToggleInstructionsExpanded)
                    },
                    onCompleteToggle = {
                        onUiEvent(RecipeDetailsUiEvent.InstructionCompletedToggled(it))
                    },
                )

                AnimatedVisibility(
                    visible = uiState.editMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    AddNewString(
                        label = "Add new instruction",
                        onAddClick = {
                            onUiEvent(RecipeDetailsUiEvent.AddInstructionClicked(it))
                        },
                    )
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = uiState.editMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    SecondaryButton(
                        text = "Discard",
                        onClick = { onUiEvent(RecipeDetailsUiEvent.DiscardClicked) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                    )
                    PrimaryButton(
                        text = "Save",
                        onClick = { onUiEvent(RecipeDetailsUiEvent.SaveClicked) },
                        modifier = Modifier.weight(1f),
                        loading = uiState.isSaving,
                    )
                }
            }
        }
    }

    if (uiState.showDeleteConfirmationDialog && uiState.recipeId != null) {
        ConfirmationDialog(
            onDismiss = {
                onUiEvent(RecipeDetailsUiEvent.DismissDeleteConfirmation)
            },
            onConfirm = {
                onUiEvent(RecipeDetailsUiEvent.ConfirmDeleteConfirmation)
            },
            dialogTitle = "Delete recipe",
            dialogMessage = "Are you sure you want to the recipe?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }

    if (uiState.showDiscardConfirmationDialog) {
        ConfirmationDialog(
            onDismiss = {
                onUiEvent(RecipeDetailsUiEvent.DismissDiscardConfirmation)
            },
            onConfirm = {
                onUiEvent(RecipeDetailsUiEvent.ConfirmDiscardConfirmation)
            },
            dialogTitle = "Discard changes?",
            dialogMessage = "Your unsaved changes will be lost.",
            dismissButtonMessage = "Keep editing",
            confirmButtonMessage = "Discard",
        )
    }

}

@Preview(showBackground = true)
@Composable
private fun RecipeDetailsScreenPreview() {
    LifeTogetherTheme {
        RecipeDetailsScreen(
            uiState = RecipeDetailsUiState.Content(
                recipeId = "recipe-1",
                familyId = "family-1",
                itemName = "Tomato Soup With chicken",
                description = "A simple soup.",
                ingredients = listOf(
                    com.example.lifetogether.domain.model.recipe.Ingredient(
                        measureType = MeasureType.PIECE,
                        itemName = "Tomatoes",
                        amount = 3.0,
                    ),
                ),
                instructions = listOf(
                    Instruction(itemName = "Blend everything"),
                ),
                preparationTimeMin = "30",
                favourite = false,
                recipeServings = 2,
                servings = "2",
                tagsInput = "Dinner Soup",
                tags = listOf("Dinner", "Soup"),
                editMode = false,
                isSaving = false,
                showDeleteConfirmationDialog = false,
                showImageUploadDialog = false,
                servingsExpanded = false,
                expandedStates = mapOf(
                    "ingredients" to true,
                    "instructions" to true,
                ),
                ingredientsByServings = listOf(
                    com.example.lifetogether.domain.model.recipe.Ingredient(
                        measureType = MeasureType.PIECE,
                        itemName = "Tomatoes",
                        amount = 3.0,
                    ),
                ),
            ),
            bitmap = null,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
