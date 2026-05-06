package com.example.lifetogether.ui.feature.lists.entryDetails.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.dialog.DatePickerDialog
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.common.textfield.DatePickerTextField
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsContent
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsUiState
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsUiEvent
import com.example.lifetogether.ui.feature.lists.entryDetails.MealPlanEntryFormState
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.text.SimpleDateFormat
import java.util.Locale

fun LazyListScope.mealPlanEntryContent(
    uiState: EntryDetailsUiState.Content,
    formState: MealPlanEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
    item {
        val showDatePicker = remember { mutableStateOf(false) }
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

        DatePickerTextField(
            label = "Date",
            date = formState.date.takeIf { it.isNotBlank() }?.let { dateFormat.parse(it) },
            onClick = { showDatePicker.value = true },
            modifier = Modifier.fillMaxWidth(),
        )

        if (showDatePicker.value) {
            DatePickerDialog(
                selectedDate = formState.date.takeIf { it.isNotBlank() }
                    ?.let { dateFormat.parse(it) },
                onDismiss = { showDatePicker.value = false },
                onDateSelected = { selected ->
                    onUiEvent(ListEntryDetailsUiEvent.Meal.DateChanged(dateFormat.format(selected)))
                    showDatePicker.value = false
                },
            )
        }
    }

    item {
        val isRecipeMode = remember { mutableStateOf( //todo add to EntryDetailsContent.Meal
            formState.recipeId.isNotEmpty() || formState.customMealName.isEmpty()
        ) }
        SecondaryTabRow(
            selectedTabIndex = if (isRecipeMode.value) 0 else 1,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Tab(
                selected = isRecipeMode.value,
                onClick = { if (uiState.isEditing) isRecipeMode.value = true },
                text = { Text("Recipe") },
            )
            Tab(
                selected = !isRecipeMode.value,
                onClick = { if (uiState.isEditing) isRecipeMode.value = false },
                text = { Text("Custom") },
            )
        }

        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.large))

        // Recipe search or custom name
        if (isRecipeMode.value) { //todo animatedcontent
            // Simple search field: typing updates recipeId (client can interpret as query or id)
            CustomTextField(
                value = formState.recipeId,
                onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Meal.RecipeIdChanged(it)) },
                label = "Search recipes by name",
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text,
                capitalization = true,
                enabled = uiState.isEditing,
            )

            // Placeholder suggestions list (to be replaced by real suggestions sourced from recipes repo)
            val suggestions = remember { listOf<Pair<String, String>>() } // Pair(id, displayText)
            if (suggestions.isNotEmpty()) { //todo real suggestions are not working
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(suggestions) { suggestion ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = uiState.isEditing) {
                                    onUiEvent(
                                        ListEntryDetailsUiEvent.Meal.RecipeIdChanged(
                                            suggestion.first
                                        )
                                    )
                                },
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = suggestion.second,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        } else {
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

    item {
        // Meal type chips
        TextSubHeadingMedium("Meal type")
        TagOptionRow(
            options = MealType.entries.map { it.displayName },
            selectedOption = formState.mealType.displayName,
            onSelectedOptionChange = {
                if (uiState.isEditing) onUiEvent(ListEntryDetailsUiEvent.Meal.MealTypeChanged(it))
            },
        )
    }

    item {
        // Notes
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

@Preview(showBackground = true)
@Composable
private fun MealPlannerEntryContentPreview() {
    LifeTogetherTheme {
        val uiState = EntryDetailsUiState.Content(
            details = EntryDetailsContent.Meal.blank(),
            isEditing = true
        )
        ListEntryDetailsContent(
            padding = PaddingValues(0.dp),
            contentState = uiState,
            isExistingEntry = false,
            onUiEvent = {},
        ) {
            mealPlanEntryContent(
                uiState = uiState,
                formState = MealPlanEntryFormState(
                    name = "",
                    date = "",
                    recipeId = "",
                    customMealName = ""
                ),
                onUiEvent = {},
            )
        }
    }
}
