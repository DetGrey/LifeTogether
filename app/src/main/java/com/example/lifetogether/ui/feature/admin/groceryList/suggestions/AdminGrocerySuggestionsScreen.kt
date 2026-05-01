package com.example.lifetogether.ui.feature.admin.groceryList.suggestions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.add.AddNewListItem
import com.example.lifetogether.ui.common.add.EditListItem
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY

@Composable
fun AdminGrocerySuggestionsScreen(
    uiState: AdminGrocerySuggestionsUiState,
    onUiEvent: (AdminGrocerySuggestionsUiEvent) -> Unit,
    onNavigationEvent: (AdminGrocerySuggestionsNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(AdminGrocerySuggestionsNavigationEvent.NavigateBack)
                },
                text = "Edit grocery list",
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                    .padding(LifeTogetherTokens.spacing.small)
                    .padding(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(horizontal = LifeTogetherTokens.spacing.xSmall),
                text = "Add a new suggestion by choosing the category (emoji) and writing the suggestion name.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextHeadingMedium("Grocery suggestions")
            if (uiState.grocerySuggestions.isNotEmpty()) {
                GrocerySuggestionsEditor(
                    uiState.grocerySuggestions,
                    expandedCategories = uiState.categoryExpandedStates,
                    onToggleExpand = { categoryName ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.ToggleCategory(categoryName))
                    },
                    onEditItem = { suggestion ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.StartEditingSuggestion(suggestion))
                    },
                    onDeleteItem = { suggestion ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.ClickDeleteSuggestion(suggestion))
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(LifeTogetherTokens.spacing.small),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (uiState.editingSuggestionId != null) {
                EditListItem(
                    textValue = uiState.newSuggestionText,
                    onTextChange = { value ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.NewSuggestionTextChanged(value))
                    },
                    priceValue = uiState.newSuggestionPrice,
                    onPriceChange = { value ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.NewSuggestionPriceChanged(value))
                    },
                    onSaveClick = {
                        onUiEvent(AdminGrocerySuggestionsUiEvent.ClickSaveSuggestion)
                    },
                    categoryList = uiState.groceryCategories,
                    selectedCategory = uiState.newSuggestionCategory,
                    onCategoryChange = { category ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.NewSuggestionCategoryChanged(category))
                    },
                )
            } else {
                AddNewListItem(
                    textValue = uiState.newSuggestionText,
                    onTextChange = { value ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.NewSuggestionTextChanged(value))
                    },
                    priceValue = uiState.newSuggestionPrice,
                    onPriceChange = { value ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.NewSuggestionPriceChanged(value))
                    },
                    onAddClick = {
                        onUiEvent(AdminGrocerySuggestionsUiEvent.ClickAddSuggestion)
                    },
                    categoryList = uiState.groceryCategories,
                    selectedCategory = uiState.newSuggestionCategory,
                    onCategoryChange = { category ->
                        onUiEvent(AdminGrocerySuggestionsUiEvent.NewSuggestionCategoryChanged(category))
                    },
                )
            }
        }

        val selectedSuggestion = uiState.selectedSuggestion
        if (uiState.showDeleteCategoryConfirmationDialog && selectedSuggestion != null) {
            ConfirmationDialog(
                onDismiss = {
                    onUiEvent(AdminGrocerySuggestionsUiEvent.DismissDeleteSuggestionDialog)
                },
                onConfirm = {
                    onUiEvent(AdminGrocerySuggestionsUiEvent.ConfirmDeleteSuggestion)
                },
                dialogTitle = "Delete category?",
                dialogMessage = "Are you sure you want to delete the category: \"${selectedSuggestion.category?.emoji} ${selectedSuggestion.category?.name} - ${selectedSuggestion.suggestionName}\"?",
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Delete",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminGrocerySuggestionsScreenPreview() {
    val category = Category(emoji = "🥦", name = "Vegetables")
    LifeTogetherTheme {
        AdminGrocerySuggestionsScreen(
            uiState = AdminGrocerySuggestionsUiState(
                groceryCategories = listOf(
                    UNCATEGORIZED_CATEGORY,
                    category,
                ),
                grocerySuggestions = listOf(
                    GrocerySuggestion(
                        id = "1",
                        category = category,
                        suggestionName = "Broccoli",
                        approxPrice = 17.5f,
                    ),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
