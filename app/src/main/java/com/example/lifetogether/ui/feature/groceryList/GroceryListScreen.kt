package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel
import com.example.lifetogether.ui.viewmodel.GroceryListViewModel

@Composable
fun GroceryListScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
) {
    val groceryListViewModel: GroceryListViewModel = viewModel()

    if (authViewModel?.userInformation?.uid != null) {
        groceryListViewModel.fetchData(authViewModel.userInformation?.uid!!)
    } else {
        groceryListViewModel.isLoading = false
    }
    // TODO remove this later
//    groceryListViewModel.updateExpandedStates()

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp),
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
                if (groceryListViewModel.isLoading) {
                    // Show a loading indicator
                    Text(text = "Loading") // TODO
                } else if (groceryListViewModel.groceryList.isEmpty()) {
                    Text(text = "No items on the list yet")
                } else {
                    groceryListViewModel.groceryCategories.forEach { category ->
                        val categoryItems = groceryListViewModel.getCategoryItems(category)
                        if (categoryItems.isNotEmpty()) {
                            groceryListViewModel.categoryExpandedStates[category.name]?.let { expanded ->
                                ItemCategoryList(
                                    category = category,
                                    itemList = categoryItems,
                                    expanded = expanded.value,
                                    onClick = {
                                        println("before: ${expanded.value}")
                                        groceryListViewModel.toggleCategoryExpanded(category.name)
                                        println("after: ${expanded.value}")
                                    },
                                )
                            }
                        }
                    }
                    val completedItems = groceryListViewModel.getCompletedItems()
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
                authViewModel?.userInformation?.uid?.let { uid ->
                    authViewModel.userInformation?.name?.let { name ->
                        groceryListViewModel.addItemToList(uid)
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroceryListScreenPreview() {
    LifeTogetherTheme {
        GroceryListScreen()
    }
}
