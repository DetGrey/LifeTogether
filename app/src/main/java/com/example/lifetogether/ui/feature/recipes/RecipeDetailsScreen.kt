package com.example.lifetogether.ui.feature.recipes

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.ui.common.add.AddNewString
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.list.CompletableCategoryList
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.tagOptionRow.TagOption
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.textfield.EditableTextField
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun RecipeDetailsScreen(
    uiState: RecipeDetailsUiState,
    bitmap: Bitmap?,
    onUiEvent: (RecipeDetailsUiEvent) -> Unit,
    onNavigationEvent: (RecipeDetailsNavigationEvent) -> Unit,
) {
    AnimatedLoadingContent(
        isLoading = uiState is RecipeDetailsUiState.Loading,
        label = "recipe_details_loading_content",
        loadingContent = {
            Skeletons.FormEdit(modifier = Modifier.fillMaxSize())
        },
    ) {
        val contentState = uiState as? RecipeDetailsUiState.Content ?: return@AnimatedLoadingContent
        RecipeDetailsContent(
            uiState = contentState,
            bitmap = bitmap,
            onUiEvent = onUiEvent,
            onNavigationEvent = onNavigationEvent,
        )
    }
}

@Composable
private fun RecipeDetailsContent(
    uiState: RecipeDetailsUiState.Content,
    bitmap: Bitmap?,
    onUiEvent: (RecipeDetailsUiEvent) -> Unit,
    onNavigationEvent: (RecipeDetailsNavigationEvent) -> Unit,
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            onUiEvent(RecipeDetailsUiEvent.RecipeImageSelected(it))
        }
    }

    val displayedBitmap = uiState.localImageBitmap ?: bitmap
    val hasRecipeId = uiState.recipeId != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LifeTogetherTokens.spacing.bottomInsetMedium)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.tertiary),
                ) {
                    if (displayedBitmap != null) {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            bitmap = displayedBitmap.asImageBitmap(),
                            contentDescription = "recipe image",
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.3f to Color.Transparent,
                                            1.0f to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.75f),
                                        ),
                                    ),
                                ),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(
                                start = LifeTogetherTokens.spacing.small,
                                top = LifeTogetherTokens.spacing.small
                            )
                            .height(40.dp)
                            .aspectRatio(1f)
                            .clickable {
                                onNavigationEvent(RecipeDetailsNavigationEvent.NavigateBack)
                            }
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back_arrow),
                            contentDescription = "back arrow icon",
                            tint = MaterialTheme.colorScheme.onTertiary,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(
                                end = LifeTogetherTokens.spacing.small,
                                top = LifeTogetherTokens.spacing.small
                            )
                            .height(40.dp)
                            .aspectRatio(1f)
                            .clickable(enabled = !uiState.editMode && hasRecipeId) {
                                onUiEvent(RecipeDetailsUiEvent.DeleteClicked)
                            }
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!uiState.editMode && hasRecipeId) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_trashcan),
                                contentDescription = "trashcan icon",
                                tint = MaterialTheme.colorScheme.onTertiary,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(
                                    start = LifeTogetherTokens.spacing.small,
                                    end = LifeTogetherTokens.spacing.xxLarge
                                ),
                        ) {
                            EditableTextField(
                                text = uiState.itemName,
                                onTextChange = { onUiEvent(RecipeDetailsUiEvent.ItemNameChanged(it)) },
                                label = "Recipe name",
                                isEditable = uiState.editMode,
                                textStyle = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary,
                                labelColor = MaterialTheme.colorScheme.onTertiary,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(LifeTogetherTokens.spacing.xSmall)
                                .height(40.dp)
                                .aspectRatio(1f)
                                .clickable {
                                    if (uiState.editMode) {
                                        imagePickerLauncher.launch("image/*")
                                    } else {
                                        onUiEvent(RecipeDetailsUiEvent.EditClicked)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (uiState.editMode) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_gallery),
                                    contentDescription = "edit icon",
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                )
                            } else if (uiState.recipeId != null) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit),
                                    contentDescription = "edit icon",
                                    tint = MaterialTheme.colorScheme.onTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LifeTogetherTokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.large),
            ) {
                if (uiState.editMode || uiState.description.isNotBlank()) {
                    EditableTextField(
                        text = uiState.description,
                        onTextChange = { onUiEvent(RecipeDetailsUiEvent.DescriptionChanged(it)) },
                        label = "Description",
                        isEditable = uiState.editMode,
                        textStyle = MaterialTheme.typography.bodyLarge,
                    )
                }

                Column {
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
                                isEditable = uiState.editMode,
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            )
                        } else {
                            Dropdown(
                                selectedValue = uiState.servings,
                                expanded = uiState.servingsExpanded,
                                onExpandedChange = {
                                    onUiEvent(RecipeDetailsUiEvent.ServingsExpandedChanged(it))
                                },
                                options = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                                    "12", "15", "20", "30", "40"),
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
                                isEditable = uiState.editMode,
                                textStyle = MaterialTheme.typography.bodySmall,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                capitalization = false,
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

                if (uiState.editMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Column {
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

                if (uiState.editMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Column {
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
    }

    Spacer(modifier = Modifier.height(30.dp))

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
