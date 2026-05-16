package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.domain.logic.minToHourMinString
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.ui.common.dialog.DatePickerDialog
import com.example.lifetogether.ui.common.list.MealPlanRecipeCard
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.common.textfield.DatePickerTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.text.SimpleDateFormat
import java.util.Locale

fun LazyListScope.mealPlanContent(
    uiState: MealPlanDetailsUiState.Content,
    familyId: String? = null,
    formState: MealPlanFormState,
    searchState: MealRecipeSearchState,
    onUiEvent: (MealPlanDetailsUiEvent) -> Unit,
    onNavigationEvent: (MealPlanDetailsNavigationEvent) -> Unit,
) {
    item {
        val showDatePicker = remember { mutableStateOf(false) }
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH) }

        DatePickerTextField(
            label = "Date",
            date = formState.date.takeIf { it.isNotBlank() }?.let { dateFormat.parse(it) },
            onClick = { showDatePicker.value = true },
            modifier = Modifier.fillMaxWidth(),
        )

        if (showDatePicker.value) {
            DatePickerDialog(
                selectedDate = formState.date.takeIf { it.isNotBlank() }?.let { dateFormat.parse(it) },
                onDismiss = { showDatePicker.value = false },
                onDateSelected = { selected ->
                    onUiEvent(MealPlanDetailsUiEvent.Meal.DateChanged(dateFormat.format(selected)))
                    showDatePicker.value = false
                },
            )
        }
    }

    item {
        Column(
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.large),
        ) {
            SecondaryTabRow(
                selectedTabIndex = if (searchState.mode == MealSearchMode.RECIPE) 0 else 1,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Tab(
                    selected = searchState.mode == MealSearchMode.RECIPE,
                    onClick = {
                        if (uiState.isEditing) {
                            onUiEvent(MealPlanDetailsUiEvent.Meal.RecipeModeChanged(MealSearchMode.RECIPE))
                        }
                    },
                    text = {
                        Text(
                            text = "Recipe",
                            color = if (searchState.mode == MealSearchMode.RECIPE) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )
                Tab(
                    selected = searchState.mode == MealSearchMode.CUSTOM,
                    onClick = {
                        if (uiState.isEditing) {
                            onUiEvent(MealPlanDetailsUiEvent.Meal.RecipeModeChanged(MealSearchMode.CUSTOM))
                        }
                    },
                    text = {
                        Text(
                            text = "Custom",
                            color = if (searchState.mode == MealSearchMode.CUSTOM) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    },
                )
            }

            AnimatedContent(targetState = searchState.mode, label = "meal_mode_content") { mode ->
                when (mode) {
                    MealSearchMode.RECIPE -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                        ) {
                            val selectedRecipe = searchState.selectedRecipeSearchItem

                            if (uiState.isEditing) {
                                CustomTextField(
                                    value = searchState.query,
                                    onValueChange = { onUiEvent(MealPlanDetailsUiEvent.Meal.RecipeQueryChanged(it)) },
                                    label = "Search recipes by name",
                                    modifier = Modifier.fillMaxWidth(),
                                    imeAction = ImeAction.Next,
                                    keyboardType = KeyboardType.Text,
                                    capitalization = true,
                                )

                                if (selectedRecipe != null && selectedRecipe.preparationTimeMin > 0) {
                                    TextDefault(
                                        text = "Prep time: ${minToHourMinString(selectedRecipe.preparationTimeMin)}",
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }

                                if (searchState.suggestions.isNotEmpty()) {
                                    RecipeSearchSuggestions(
                                        suggestions = searchState.suggestions,
                                        onSuggestionClick = { suggestion ->
                                            onUiEvent(MealPlanDetailsUiEvent.Meal.RecipeSelected(suggestion))
                                        },
                                    )
                                }
                            } else if (selectedRecipe != null && familyId != null) {
                                MealPlanRecipeCard(
                                    familyId = familyId,
                                    recipeId = selectedRecipe.id,
                                    recipeName = selectedRecipe.itemName,
                                    mealType = formState.mealType.displayName,
                                    prepTimeMin = selectedRecipe.preparationTimeMin,
                                    onClick = {
                                        onNavigationEvent(
                                            MealPlanDetailsNavigationEvent.NavigateToRecipeDetails(selectedRecipe.id),
                                        )
                                    },
                                )
                            } else {
                                TextDefault(
                                    text = "Recipe",
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                    MealSearchMode.CUSTOM -> {
                        CustomTextField(
                            value = formState.customMealName,
                            onValueChange = { onUiEvent(MealPlanDetailsUiEvent.Meal.CustomMealNameChanged(it)) },
                            label = "Meal name",
                            modifier = Modifier.fillMaxWidth(),
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                            capitalization = true,
                            enabled = uiState.isEditing,
                        )
                    }
                }
            }
        }
    }

    item {
        TextSubHeadingMedium("Meal type")
        val options = if (uiState.isEditing) MealType.entries.map { it.displayName }
            else listOf(formState.mealType.displayName)
        TagOptionRow(
            options = options,
            selectedOption = formState.mealType.displayName,
            onSelectedOptionChange = {
                if (uiState.isEditing) onUiEvent(MealPlanDetailsUiEvent.Meal.MealTypeChanged(it))
             },
        )
    }

    item {
        CustomTextField(
            value = formState.notes,
            onValueChange = { onUiEvent(MealPlanDetailsUiEvent.Meal.NotesChanged(it)) },
            label = "Notes (optional)",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Default,
            keyboardType = KeyboardType.Text,
            capitalization = true,
            enabled = uiState.isEditing,
        )
    }
}

@Composable
private fun RecipeSearchSuggestions(
    suggestions: List<RecipeSearchItem>,
    onSuggestionClick: (RecipeSearchItem) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
    ) {
        suggestions.forEach { suggestion ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSuggestionClick(suggestion) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                ),
                shape = MaterialTheme.shapes.large,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = LifeTogetherTokens.spacing.medium,
                            vertical = LifeTogetherTokens.spacing.small,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = suggestion.itemName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    Text(
                        text = minToHourMinString(suggestion.preparationTimeMin),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MealPlanContentPreview() {
    LifeTogetherTheme {
        Column {
            // Preview intentionally left minimal; full preview comes from the screen.
        }
    }
}
