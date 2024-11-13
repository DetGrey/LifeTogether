package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.feature.groceryList.AddNewListItem
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel

@Composable
fun AdminGrocerySuggestionsScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
) {
    val grocerySuggestionsViewModel: AdminGrocerySuggestionsViewModel = hiltViewModel()

    val userInformation by authViewModel?.userInformation!!.collectAsState()
    val groceryCategories by grocerySuggestionsViewModel.groceryCategories.collectAsState()
    val grocerySuggestions by grocerySuggestionsViewModel.grocerySuggestions.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        grocerySuggestionsViewModel.setUpGrocerySuggestions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
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
            }

            item {
                TextHeadingMedium("Grocery suggestions")
                if (grocerySuggestions.isNotEmpty()) {
                    ListEditorContainer(
                        grocerySuggestions.map { suggestion -> "${suggestion.category?.emoji} ${suggestion.category?.name} - ${suggestion.suggestionName}" },
                        onDelete = { suggestionString ->
                            val suggestionList = suggestionString.split(" - ", limit = 2)
                            val categoryList = suggestionList[0].split(" ", limit = 2)
                            val category = Category(categoryList[0], categoryList[1])

                            val suggestion = grocerySuggestions.find { it.category == category && it.suggestionName == suggestionList[1] }

                            if (suggestion != null) {
                                grocerySuggestionsViewModel.selectedSuggestion = suggestion
                                grocerySuggestionsViewModel.showDeleteCategoryConfirmationDialog = true
                            }
                        },
                    )
                }
            }

            item {
                Text(
                    text = "Add new category as a string with an emoji and a name with whitespace between e.g. \"\uD83C\uDF5E Bakery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                )
                Spacer(modifier = Modifier.height(20.dp))

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
        }
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
        AdminGrocerySuggestionsScreen()
    }
}
