package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.add.AddNewListItem
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.list.ItemCategoryList
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY_NAME
import com.example.lifetogether.util.priceToString

@Composable
fun GroceryListScreen(
    uiState: GroceryListUiState,
    onUiEvent: (GroceryListUiEvent) -> Unit,
    onNavigationEvent: (GroceryListNavigationEvent) -> Unit,
) {
    val contentState = uiState as? GroceryListUiState.Content

    Scaffold(
        topBar = {
            AppTopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(GroceryListNavigationEvent.NavigateBack)
                },
                text = "Grocery list",
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is GroceryListUiState.Loading,
            label = "grocery_list_loading_content",
            loadingContent = {
                Skeletons.ListDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = contentState ?: return@AnimatedLoadingContent
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(LifeTogetherTokens.spacing.small)
                    .padding(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {
                        if (content.groceryList.isEmpty()) {
                            TextDefault(text = "No items on the list yet")
                        } else {
                            content.expectedTotalPrice?.let {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    TextSubHeadingMedium(
                                        text = "Expected total price:",
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                    TextSubHeadingMedium(
                                        text = it.priceToString(true),
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }

                            content.categorizedItems.forEach { (category, groceryItems) ->
                                if (groceryItems.isNotEmpty()) {
                                    content.categoryExpandedStates[category.name]?.let { expanded ->
                                        ItemCategoryList(
                                            category = category,
                                            itemList = groceryItems,
                                            expanded = expanded,
                                            onClick = {
                                                onUiEvent(GroceryListUiEvent.CategoryExpandedClicked(category.name))
                                            },
                                            onCompleteToggle = { item ->
                                                if (item is GroceryItem) {
                                                    onUiEvent(GroceryListUiEvent.ItemCompletedToggled(item))
                                                }
                                            },
                                            onBellClick = { item ->
                                                onUiEvent(GroceryListUiEvent.NotificationClicked(item))
                                            },
                                        )
                                    }
                                }
                            }

                            if (content.completedItems.isNotEmpty()) {
                                ItemCategoryList(
                                    category = Category(
                                        emoji = "✔️",
                                        name = "Completed",
                                    ),
                                    itemList = content.completedItems,
                                    expanded = content.completedSectionExpanded == true,
                                    onClick = {
                                        onUiEvent(GroceryListUiEvent.CompletedSectionExpandedClicked)
                                    },
                                    onCompleteToggle = { item ->
                                        if (item is GroceryItem) {
                                            onUiEvent(GroceryListUiEvent.ItemCompletedToggled(item))
                                        }
                                    },
                                    onBellClick = { item ->
                                        onUiEvent(GroceryListUiEvent.NotificationClicked(item))
                                    },
                                    onDelete = {
                                        onUiEvent(GroceryListUiEvent.DeleteCompletedClicked)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = contentState?.currentGrocerySuggestions.orEmpty().isNotEmpty(),
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            Box(
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.small)
                    .padding(bottom = LifeTogetherTokens.spacing.xLarge)
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                GrocerySuggestionPopup(
                    suggestions = contentState?.currentGrocerySuggestions.orEmpty(),
                    onClick = { suggestion ->
                        onUiEvent(GroceryListUiEvent.SuggestionClicked(suggestion))
                    },
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(LifeTogetherTokens.spacing.small)
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            AddNewListItem(
                textValue = contentState?.newItemText.orEmpty(),
                onTextChange = { onUiEvent(GroceryListUiEvent.NewItemTextChanged(it)) },
                priceValue = contentState?.newItemPrice.orEmpty(),
                onPriceChange = { onUiEvent(GroceryListUiEvent.NewItemPriceChanged(it)) },
                onAddClick = { onUiEvent(GroceryListUiEvent.AddItemClicked) },
                categoryList = contentState?.groceryCategories.orEmpty(),
                selectedCategory = contentState?.newItemCategory ?: UNCATEGORIZED_CATEGORY,
                onCategoryChange = { newCategory ->
                    onUiEvent(GroceryListUiEvent.NewItemCategoryChanged(newCategory))
                },
            )
        }

        if (contentState?.showConfirmationDialog == true) {
            ConfirmationDialog(
                onDismiss = { onUiEvent(GroceryListUiEvent.DismissDeleteCompletedConfirmation) },
                onConfirm = { onUiEvent(GroceryListUiEvent.ConfirmDeleteCompletedConfirmation) },
                dialogTitle = "Delete completed items",
                dialogMessage = "Are you sure you want to delete all completed grocery items?",
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Delete",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GroceryListScreenPreview() {
    LifeTogetherTheme {
        GroceryListScreen(
            uiState = GroceryListUiState.Loading,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroceryListScreenPreview2() {
    LifeTogetherTheme {
        GroceryListScreen(
            uiState = GroceryListUiState.Content(
                groceryList = listOf(
                    GroceryItem(
                        id = "4",
                        itemName = "Apple",
                        approxPrice = 12.0F
                    ),
                    GroceryItem(
                        id = "5",
                        itemName = "Apple",
                    ),
                ),
                categorizedItems = mapOf(
                    UNCATEGORIZED_CATEGORY to listOf(
                        GroceryItem(
                            id = "1",
                            itemName = "Apple",
                            approxPrice = 12.0F
                        ),
                        GroceryItem(
                            id = "2",
                            itemName = "Banana",
                        ),
                    )
                ),
                completedItems = listOf(
                    GroceryItem(
                        id = "3",
                        itemName = "Banana",
                    )
                ),
                groceryCategories = listOf(UNCATEGORIZED_CATEGORY),
                categoryExpandedStates = mapOf(
                    UNCATEGORIZED_CATEGORY_NAME to true,
                ),
                expectedTotalPrice = 40.0F,
                allGrocerySuggestions = emptyList(),
                currentGrocerySuggestions = emptyList(),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
