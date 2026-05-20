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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.ingredientMatchesSuggestion
import com.example.lifetogether.domain.model.recipe.Ingredient
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
import sh.calvin.reorderable.ReorderableListItemScope

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
        onUiEvent(RecipeDetailsUiEvent.DialogEvent.DiscardClicked)
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
                        onUiEvent(RecipeDetailsUiEvent.DialogEvent.DiscardClicked)
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
                    { onUiEvent(RecipeDetailsUiEvent.DialogEvent.DeleteClicked) }
                } else if (showEditAction) {
                    { onUiEvent(RecipeDetailsUiEvent.Editor.EditClicked) }
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
            onUiEvent(RecipeDetailsUiEvent.Editor.RecipeImageSelected(it))
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
                    onTextChange = { onUiEvent(RecipeDetailsUiEvent.Editor.ItemNameChanged(it)) },
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
                    onTextChange = { onUiEvent(RecipeDetailsUiEvent.Editor.DescriptionChanged(it)) },
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
                            onUiEvent(RecipeDetailsUiEvent.Editor.PreparationTimeChanged(it))
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
                                onUiEvent(RecipeDetailsUiEvent.Editor.ServingsChanged(it))
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
                                onUiEvent(RecipeDetailsUiEvent.Editor.ServingsExpandedChanged(it))
                            },
                            options = recipeServingsOptions,
                            label = null,
                            onValueChangedEvent = {
                                onUiEvent(RecipeDetailsUiEvent.Editor.ServingsChanged(it))
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
                                onUiEvent(RecipeDetailsUiEvent.Editor.TagsChanged(it))
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
                        onUiEvent(RecipeDetailsUiEvent.Editor.ToggleIngredientsExpanded)
                    },
                    onCompleteToggle = {
                        onUiEvent(RecipeDetailsUiEvent.IngredientEvent.CompletedToggled(it))
                    },
                    onReorder = if (uiState.editMode) { fromIndex, toIndex ->
                        onUiEvent(RecipeDetailsUiEvent.IngredientEvent.Moved(fromIndex, toIndex))
                    } else null,
                    trailingContent = if (!uiState.editMode) { _, item, _ ->
                        val hasMatch = item is Ingredient && uiState.grocerySuggestions.any { suggestion ->
                            ingredientMatchesSuggestion(item.itemName, suggestion.suggestionName)
                        }
                        if (hasMatch) {
                            {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_grocery),
                                    contentDescription = "Add to grocery list",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .size(LifeTogetherTokens.sizing.iconLarge)
                                        .clickable {
                                            onUiEvent(RecipeDetailsUiEvent.IngredientEvent.AddToGroceryList(item))
                                        },
                                )
                            }
                        } else null
                    } else { _, item, scope ->
                        {
                            RecipeItemEditActions(
                                isCancelling = uiState.editingIngredientId == item.id,
                                onEditClick = {
                                    if (uiState.editingIngredientId == item.id) {
                                        onUiEvent(RecipeDetailsUiEvent.IngredientEvent.CancelEdit)
                                    } else {
                                        onUiEvent(RecipeDetailsUiEvent.IngredientEvent.EditClicked(item.id))
                                    }
                                },
                                reorderScope = scope,
                            )
                        }
                    },
                )
            }

            AnimatedVisibility(
                visible = uiState.editMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                AddNewIngredient(
                    itemName = uiState.ingredientDraft.itemName,
                    onItemNameChange = {
                        onUiEvent(RecipeDetailsUiEvent.IngredientEvent.NameChanged(it))
                    },
                    amount = uiState.ingredientDraft.amount,
                    onAmountChange = {
                        onUiEvent(RecipeDetailsUiEvent.IngredientEvent.AmountChanged(it))
                    },
                    measureType = uiState.ingredientDraft.measureType,
                    onMeasureTypeChange = {
                        onUiEvent(RecipeDetailsUiEvent.IngredientEvent.MeasureTypeChanged(it))
                    },
                    actionLabel = if (uiState.editingIngredientId != null) "Save" else "Add",
                    onActionClick = {
                        onUiEvent(
                            RecipeDetailsUiEvent.IngredientEvent.AddClicked(
                                Ingredient(
                                    amount = uiState.ingredientDraft.amount.toDoubleOrNull() ?: 0.0,
                                    measureType = uiState.ingredientDraft.measureType,
                                    itemName = uiState.ingredientDraft.itemName,
                                ),
                            ),
                        )
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
                        onUiEvent(RecipeDetailsUiEvent.Editor.ToggleInstructionsExpanded)
                    },
                    onCompleteToggle = {
                        onUiEvent(RecipeDetailsUiEvent.InstructionEvent.CompletedToggled(it))
                    },
                    onReorder = if (uiState.editMode) { fromIndex, toIndex ->
                        onUiEvent(RecipeDetailsUiEvent.InstructionEvent.Moved(fromIndex, toIndex))
                    } else null,
                    trailingContent = if (uiState.editMode) { _, item, scope ->
                        {
                            RecipeItemEditActions(
                                isCancelling = uiState.editingInstructionId == item.id,
                                onEditClick = {
                                    if (uiState.editingInstructionId == item.id) {
                                        onUiEvent(RecipeDetailsUiEvent.InstructionEvent.CancelEdit)
                                    } else {
                                        onUiEvent(RecipeDetailsUiEvent.InstructionEvent.EditClicked(item.id))
                                    }
                                },
                                reorderScope = scope,
                            )
                        }
                    } else null,
                )

                AnimatedVisibility(
                    visible = uiState.editMode,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    AddNewString(
                        label = if (uiState.editingInstructionId != null) "Edit instruction" else "Add new instruction",
                        textValue = uiState.instructionDraft,
                        onTextChange = {
                            onUiEvent(RecipeDetailsUiEvent.InstructionEvent.TextChanged(it))
                        },
                        actionLabel = if (uiState.editingInstructionId != null) "Save" else "Add",
                        onAddClick = {
                            onUiEvent(RecipeDetailsUiEvent.InstructionEvent.AddClicked(uiState.instructionDraft))
                        },
                        showTwoLines = true,
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
                        onClick = { onUiEvent(RecipeDetailsUiEvent.DialogEvent.DiscardClicked) },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSaving,
                    )
                    PrimaryButton(
                        text = "Save",
                        onClick = { onUiEvent(RecipeDetailsUiEvent.DialogEvent.SaveClicked) },
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
                onUiEvent(RecipeDetailsUiEvent.DialogEvent.DismissDeleteConfirmation)
            },
            onConfirm = {
                onUiEvent(RecipeDetailsUiEvent.DialogEvent.ConfirmDeleteConfirmation)
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
                onUiEvent(RecipeDetailsUiEvent.DialogEvent.DismissDiscardConfirmation)
            },
            onConfirm = {
                onUiEvent(RecipeDetailsUiEvent.DialogEvent.ConfirmDiscardConfirmation)
            },
            dialogTitle = "Discard changes?",
            dialogMessage = "Your unsaved changes will be lost.",
            dismissButtonMessage = "Keep editing",
            confirmButtonMessage = "Discard",
        )
    }

}

@Composable
private fun RecipeItemEditActions(
    isCancelling: Boolean,
    onEditClick: () -> Unit,
    reorderScope: ReorderableListItemScope?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.ic_edit),
            contentDescription = if (isCancelling) "Cancel item edit" else "Edit item",
            tint = if (isCancelling) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onBackground
            },
            modifier = Modifier
                .size(LifeTogetherTokens.sizing.iconLarge)
                .clickable { onEditClick() },
        )
        Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.xSmall))
        if (reorderScope != null) {
            Icon(
                painter = painterResource(id = R.drawable.ic_drag_handle),
                contentDescription = "Reorder item",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = with(reorderScope) {
                    Modifier
                        .size(LifeTogetherTokens.sizing.iconLarge)
                        .longPressDraggableHandle()
                },
            )
        }
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
                    Ingredient(
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
                    Ingredient(
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
