package com.example.lifetogether.ui.feature.lists.entryDetails.content

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.minToHourMinString
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.ui.common.dialog.DatePickerDialog
import com.example.lifetogether.ui.common.list.MealPlanRecipeCard
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.common.textfield.DatePickerTextField
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsContent
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsUiState
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsNavigationEvent
import com.example.lifetogether.ui.feature.lists.entryDetails.MealSearchMode
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsUiEvent
import com.example.lifetogether.ui.feature.lists.entryDetails.MealPlanEntryFormState
import com.example.lifetogether.ui.feature.lists.entryDetails.MealRecipeSearchState
import com.example.lifetogether.ui.feature.lists.entryDetails.RecipeSearchItem
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.text.SimpleDateFormat
import java.util.Locale

fun LazyListScope.mealPlanEntryContent(
    uiState: EntryDetailsUiState.Content,
    familyId: String? = null,
    formState: MealPlanEntryFormState,
    searchState: MealRecipeSearchState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
    onNavigationEvent: (ListEntryDetailsNavigationEvent) -> Unit,
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
                    onUiEvent(ListEntryDetailsUiEvent.Meal.DateChanged(dateFormat.format(selected)))
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
                            onUiEvent(ListEntryDetailsUiEvent.Meal.RecipeModeChanged(MealSearchMode.RECIPE))
                        }
                    },
                    text = { Text("Recipe") },
                )
                Tab(
                    selected = searchState.mode == MealSearchMode.CUSTOM,
                    onClick = {
                        if (uiState.isEditing) {
                            onUiEvent(ListEntryDetailsUiEvent.Meal.RecipeModeChanged(MealSearchMode.CUSTOM))
                        }
                    },
                    text = { Text("Custom") },
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
                                    onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Meal.RecipeQueryChanged(it)) },
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
                                            onUiEvent(ListEntryDetailsUiEvent.Meal.RecipeSelected(suggestion))
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
                                            ListEntryDetailsNavigationEvent.NavigateToRecipeDetails(selectedRecipe.id),
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
                            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Meal.CustomMealNameChanged(it)) },
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
        TagOptionRow(
            options = MealType.entries.map { it.displayName },
            selectedOption = formState.mealType.displayName,
            onSelectedOptionChange = {
                if (uiState.isEditing) {
                    onUiEvent(ListEntryDetailsUiEvent.Meal.MealTypeChanged(it))
                }
            },
        )
    }

    item {
        CustomTextField(
            value = formState.notes,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Meal.NotesChanged(it)) },
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
private fun MealPlannerEntryContentPreview() {
    LifeTogetherTheme {
        ListEntryDetailsContent(
            padding = PaddingValues(0.dp),
            contentState = EntryDetailsUiState.Content(
                details = EntryDetailsContent.Meal.blank(),
                mealRecipeSearchState = MealRecipeSearchState(
                    mode = MealSearchMode.RECIPE,
                    query = "tom",
                    isSearchFocused = true,
                    suggestions = listOf(
                        RecipeSearchItem(
                            id = "recipe-1",
                            itemName = "Tomato Soup",
                            preparationTimeMin = 25,
                        ),
                        RecipeSearchItem(
                            id = "recipe-2",
                            itemName = "Tomato Pasta",
                            preparationTimeMin = 35,
                        ),
                    ),
                ),
                isEditing = true,
            ),
            isExistingEntry = false,
            onUiEvent = {},
        ) {
            mealPlanEntryContent(
                uiState = EntryDetailsUiState.Content(
                    details = EntryDetailsContent.Meal.blank(),
                    mealRecipeSearchState = MealRecipeSearchState(
                        mode = MealSearchMode.RECIPE,
                        query = "tom",
                        isSearchFocused = true,
                        suggestions = listOf(
                            RecipeSearchItem(
                                id = "recipe-1",
                                itemName = "Tomato Soup",
                                preparationTimeMin = 25,
                            ),
                        ),
                    ),
                    isEditing = true,
                ),
                formState = MealPlanEntryFormState(
                    name = "tom",
                    date = "2026-05-08",
                    recipeId = "",
                    customMealName = "",
                    mealType = MealType.DINNER,
                    notes = "",
                ),
                searchState = MealRecipeSearchState(
                    mode = MealSearchMode.RECIPE,
                    query = "tom",
                    isSearchFocused = true,
                    suggestions = listOf(
                        RecipeSearchItem(
                            id = "recipe-1",
                            itemName = "Tomato Soup",
                            preparationTimeMin = 25,
                        ),
                    ),
                ),
                onUiEvent = {},
                onNavigationEvent = {},
            )
        }
    }
}
