package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.add.AddNewListItem
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.observer.ObserverUpdatingText
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel
import com.example.lifetogether.domain.observer.ObserverKey

@Composable
fun AdminGrocerySuggestionsScreen(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
) {
    val grocerySuggestionsViewModel: AdminGrocerySuggestionsViewModel = hiltViewModel()

    val groceryCategories by grocerySuggestionsViewModel.groceryCategories.collectAsState()
    val categoryExpandedStates by grocerySuggestionsViewModel.categoryExpandedStates.collectAsState()
    val grocerySuggestions by grocerySuggestionsViewModel.grocerySuggestions.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        grocerySuggestionsViewModel.setUpGrocerySuggestions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    appNavigator?.navigateBack()
                },
                text = "Edit grocery list",
            )

            ObserverUpdatingText(
                appSessionViewModel = appSessionViewModel,
                keys = setOf(ObserverKey.GROCERY_CATEGORIES, ObserverKey.GROCERY_SUGGESTIONS),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                modifier = Modifier.padding(horizontal = 5.dp),
                text = "Add a new suggestion by choosing the category (emoji) and writing the suggestion name.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextHeadingMedium("Grocery suggestions")
            if (grocerySuggestions.isNotEmpty()) {
                GrocerySuggestionsEditor(
                    grocerySuggestions,
                    expandedCategories = categoryExpandedStates,
                    onToggleExpand = { grocerySuggestionsViewModel.toggleCategory(it) },
                    onDeleteItem = {
                        grocerySuggestionsViewModel.selectedSuggestion = it
                        grocerySuggestionsViewModel.showDeleteCategoryConfirmationDialog = true
                    },
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AddNewListItem(
            textValue = grocerySuggestionsViewModel.newSuggestionText,
            onTextChange = { grocerySuggestionsViewModel.newSuggestionText = it },
            onAddClick = {
                grocerySuggestionsViewModel.addNewGrocerySuggestion()
            },
            categoryList = groceryCategories,
            selectedCategory = grocerySuggestionsViewModel.newSuggestionCategory,
            onCategoryChange = { newCategory ->
                grocerySuggestionsViewModel.updateNewSuggestionCategory(newCategory)
            },
        )
    }

    if (grocerySuggestionsViewModel.showDeleteCategoryConfirmationDialog && grocerySuggestionsViewModel.selectedSuggestion != null) {
        ConfirmationDialog(
            onDismiss = { grocerySuggestionsViewModel.showDeleteCategoryConfirmationDialog = false },
            onConfirm = {
                grocerySuggestionsViewModel.deleteCategory()
            },
            dialogTitle = "Delete category?",
            dialogMessage = "Are you sure you want to delete the category: \"${grocerySuggestionsViewModel.selectedSuggestion!!.category?.emoji} ${grocerySuggestionsViewModel.selectedSuggestion!!.category?.name} - ${grocerySuggestionsViewModel.selectedSuggestion!!.suggestionName}\"?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }

    if (grocerySuggestionsViewModel.showAlertDialog) {
        ErrorAlertDialog(grocerySuggestionsViewModel.error)
        grocerySuggestionsViewModel.toggleAlertDialog()
    }
}

@Preview(showBackground = true)
@Composable
fun AdminGrocerySuggestionsScreenPreview() {
    LifeTogetherTheme {
        Text("Preview requires AppSessionViewModel")
    }
}
