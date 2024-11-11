package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.enums.UpdateType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel
import com.example.lifetogether.ui.viewmodel.GroceryListViewModel

@Composable
fun GroceryListScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
) {
    val groceryListViewModel: GroceryListViewModel = hiltViewModel()

    val userInformationState by authViewModel?.userInformation!!.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        println("GroceryList familyId: ${userInformationState?.familyId}")
        userInformationState?.familyId?.let { groceryListViewModel.setUpGroceryList(it) }
    }

    // Collecting the StateFlows as state
    val groceryCategories by groceryListViewModel.groceryCategories.collectAsState()
    val categoryExpandedStates by groceryListViewModel.categoryExpandedStates.collectAsState()
    val groceryList by groceryListViewModel.groceryList.collectAsState()
    val categorizedItems by groceryListViewModel.categorizedItems.collectAsState()
    val completedItems by groceryListViewModel.completedItems.collectAsState()

//    if (groceryListViewModel.isLoading) {
//        // Show a loading indicator
//        Text(text = "Loading") // TODO
//    } else {
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
                if (groceryList.isEmpty()) {
                    Text(text = "No items on the list yet")
                } else {
                    categorizedItems.forEach { (category, groceryItems) ->
//                            println("Screen: categorizedItems category: $category")
//                            println("Screen: categorizedItems items: $groceryItems")
                        if (groceryItems.isNotEmpty()) {
                            categoryExpandedStates[category.name]?.let { expanded ->
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
                                        groceryListViewModel.toggleItemCompleted(item)
                                    },
                                )
                            }
                        }
                    }

                    if (completedItems.isNotEmpty()) {
                        ItemCategoryList(
                            category = Category(
                                emoji = "✔️",
                                name = "Completed",
                            ),
                            itemList = completedItems,
                            expanded = groceryListViewModel.completedSectionExpanded,
                            onClick = {
                                groceryListViewModel.completedSectionExpanded =
                                    !groceryListViewModel.completedSectionExpanded
                            },
                            onCompleteToggle = { item ->
                                groceryListViewModel.toggleItemCompleted(item)
                                if (!item.completed) { // if it was not completed, but now will be
                                    authViewModel?.updateItemCount("grocery-list", UpdateType.SUBTRACT)
                                } else {
                                    authViewModel?.updateItemCount("grocery-list", UpdateType.ADD)
                                }
                            },
                            onDelete = {
                                groceryListViewModel.showConfirmationDialog = true
                            },
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .padding(10.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AddNewListItem(
            textValue = groceryListViewModel.newItemText,
            onTextChange = { groceryListViewModel.newItemText = it },
            onAddClick = {
                groceryListViewModel.addItemToList(onSuccess = {
                    authViewModel?.updateItemCount("grocery-list", UpdateType.ADD)
                })
            },
            categoryList = groceryCategories,
            selectedCategory = groceryListViewModel.newItemCategory,
            onCategoryChange = { newCategory ->
                groceryListViewModel.updateNewItemCategory(newCategory)
            },
        )
    }
//    }


    if (groceryListViewModel.showConfirmationDialog) {
        ConfirmationDialog(
            onDismiss = { groceryListViewModel.showConfirmationDialog = false },
            onConfirm = {
                groceryListViewModel.deleteCompletedItems()
            },
            dialogTitle = "Delete completed items",
            dialogMessage = "Are you sure you want to delete all completed grocery items?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }

    if (groceryListViewModel.showAlertDialog) {
        ErrorAlertDialog(groceryListViewModel.error)
        groceryListViewModel.toggleAlertDialog()
    }
}

@Preview(showBackground = true)
@Composable
fun GroceryListScreenPreview() {
    LifeTogetherTheme {
        GroceryListScreen()
    }
}
