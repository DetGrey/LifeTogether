package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.add.AddNewListItem
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.list.ItemCategoryList
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.util.priceToString

@Composable
fun GroceryListScreen(
    appNavigator: AppNavigator? = null,
) {
    val groceryListViewModel: GroceryListViewModel = hiltViewModel()

    val uiState by groceryListViewModel.uiState.collectAsState()

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
                    text = "Grocery list",
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncUpdatingText(
                        keys = setOf(
                            SyncKey.GROCERY_LIST,
                            SyncKey.GROCERY_CATEGORIES,
                            SyncKey.GROCERY_SUGGESTIONS,
                        ),
                    )

                    if (uiState.groceryList.isEmpty()) {
                        Text(text = "No items on the list yet")
                    } else {

                        uiState.expectedTotalPrice?.let {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TextSubHeadingMedium(
                                    text = "Expected total price:",
                                )
                                TextSubHeadingMedium(
                                    text = it.priceToString(true),
                                )
                            }
                        }

                        uiState.categorizedItems.forEach { (category, groceryItems) ->
                            if (groceryItems.isNotEmpty()) {
                                uiState.categoryExpandedStates[category.name]?.let { expanded ->
                                    ItemCategoryList(
                                        category = category,
                                        itemList = groceryItems,
                                        expanded = expanded,
                                        onClick = {
                                            println("before: $expanded")
                                            groceryListViewModel.toggleCategoryExpanded(category.name)
                                            println("after: $expanded")
                                        },
                                        onCompleteToggle = { item ->
                                            if (item is GroceryItem) {
                                                groceryListViewModel.toggleItemCompleted(item)
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        if (uiState.completedItems.isNotEmpty()) {
                            ItemCategoryList(
                                category = Category(
                                    emoji = "✔️",
                                    name = "Completed",
                                ),
                                itemList = uiState.completedItems,
                                expanded = uiState.completedSectionExpanded,
                                onClick = {
                                    groceryListViewModel.toggleCompletedSectionExpanded()
                                },
                                onCompleteToggle = { item ->
                                    if (item is GroceryItem) {
                                        groceryListViewModel.toggleItemCompleted(item)
                                    }
                                },
                                onDelete = {
                                    groceryListViewModel.showDeleteCompletedConfirmation()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- GROCERY SUGGESTIONS POPUP
    if (uiState.currentGrocerySuggestions.isNotEmpty()) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .padding(bottom = 30.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            GrocerySuggestionPopup(
                suggestions = uiState.currentGrocerySuggestions,
                onClick = {
                    groceryListViewModel.applySuggestion(it)
                    groceryListViewModel.addItemToList()
                },
            )
        }
    }

    // ---------------------------------------------------------------- ADD NEW GROCERY ITEM
    Box(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AddNewListItem(
            textValue = uiState.newItemText,
            onTextChange = { groceryListViewModel.onNewItemTextChange(it) },
            priceValue = uiState.newItemPrice,
            onPriceChange = { groceryListViewModel.onNewItemPriceChange(it) },
            onAddClick = { groceryListViewModel.addItemToList() },
            categoryList = uiState.groceryCategories,
            selectedCategory = uiState.newItemCategory,
            onCategoryChange = { newCategory ->
                groceryListViewModel.updateNewItemCategory(newCategory)
            },
        )
    }

    // ---------------------------------------------------------------- CONFIRM DELETION OF COMPLETED ITEMS
    if (uiState.showConfirmationDialog) {
        ConfirmationDialog(
            onDismiss = { groceryListViewModel.dismissDeleteCompletedConfirmation() },
            onConfirm = {
                groceryListViewModel.deleteCompletedItems()
            },
            dialogTitle = "Delete completed items",
            dialogMessage = "Are you sure you want to delete all completed grocery items?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (uiState.showAlertDialog) {
        LaunchedEffect(uiState.error) {
            groceryListViewModel.toggleAlertDialog()
        }
        ErrorAlertDialog(uiState.error)
    }
}
