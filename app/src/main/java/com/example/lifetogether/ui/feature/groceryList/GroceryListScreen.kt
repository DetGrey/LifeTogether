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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.logic.toAbbreviatedDateString
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.add.AddNewListItem
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.feature.groceryList.components.GroceryListItem
import com.example.lifetogether.ui.feature.groceryList.components.GroceryCategoryList
import com.example.lifetogether.ui.feature.groceryList.components.GroceryCategoryListHeader
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.feature.groceryList.components.GrocerySuggestionPopup
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY_NAME
import com.example.lifetogether.util.priceToString
import kotlin.math.min

private const val COMPLETED_ITEMS_PAGE_SIZE = 10

@Composable
fun GroceryListScreen(
    uiState: GroceryListUiState,
    onUiEvent: (GroceryListUiEvent) -> Unit,
    onNavigationEvent: (GroceryListNavigationEvent) -> Unit,
) {
    val contentState = uiState as? GroceryListUiState.Content
    var showDeleteCompletedDialog by remember { mutableStateOf(false) }
    var visibleCompletedItemsCount by remember { mutableIntStateOf(COMPLETED_ITEMS_PAGE_SIZE) }

    LaunchedEffect(contentState?.completedSectionExpanded, contentState?.completedItems?.size) {
        val content = contentState ?: return@LaunchedEffect
        visibleCompletedItemsCount = if (!content.completedSectionExpanded) {
            COMPLETED_ITEMS_PAGE_SIZE
        } else {
            min(
                visibleCompletedItemsCount,
                content.completedItems.size,
            ).coerceAtLeast(COMPLETED_ITEMS_PAGE_SIZE)
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
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
                    .padding(LifeTogetherTokens.spacing.small),
                contentPadding = PaddingValues(
                    bottom = LifeTogetherTokens.spacing.bottomInsetLarge * 2
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
            ) {
                if (content.groceryList.isEmpty()) {
                    item {
                        TextDefault(text = "No items on the list yet")
                    }
                } else {
                    content.expectedTotalPrice?.let {
                        item {
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
                    }

                    content.categorizedItems.forEach { (category, groceryItems) ->
                        if (groceryItems.isNotEmpty()) {
                            content.categoryExpandedStates[category.name]?.let { expanded ->
                                item {
                                    GroceryCategoryList(
                                        category = category,
                                        itemList = groceryItems,
                                        expanded = expanded,
                                        onClick = {
                                            onUiEvent(GroceryListUiEvent.CategoryExpandedClicked(category.name))
                                        },
                                        onCompleteToggle = { item ->
                                            onUiEvent(GroceryListUiEvent.ItemCompletedToggled(item))
                                        },
                                        onBellClick = { item ->
                                            onUiEvent(GroceryListUiEvent.NotificationClicked(item))
                                        },
                                    )
                                }
                            }
                        }
                    }

                    if (content.completedItems.isNotEmpty()) {
                        item {
                            GroceryCategoryListHeader(
                                category = Category(
                                    emoji = "✔️",
                                    name = "Bought",
                                ),
                                expanded = content.completedSectionExpanded,
                                onClick = {
                                    onUiEvent(GroceryListUiEvent.CompletedSectionExpandedClicked)
                                },
                                onDelete = { showDeleteCompletedDialog = true },
                            )

                            if (content.completedSectionExpanded) {
                                Column(
                                    modifier = Modifier.padding(top = LifeTogetherTokens.spacing.small),
                                ) {
                                    content.completedItems
                                        .take(visibleCompletedItemsCount)
                                        .forEach { item ->
                                            GroceryListItem(
                                                item = item,
                                                onCompleteToggle = {
                                                    onUiEvent(GroceryListUiEvent.ItemCompletedToggled(item))
                                                },
                                                trailingText = item.lastUpdated.toAbbreviatedDateString(),
                                                onBellClick = null,
                                            )
                                        }

                                    if (visibleCompletedItemsCount < content.completedItems.size) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    visibleCompletedItemsCount = min(
                                                        visibleCompletedItemsCount + COMPLETED_ITEMS_PAGE_SIZE,
                                                        content.completedItems.size,
                                                    )
                                                },
                                            ) {
                                                Text(
                                                    text = "Show more",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(LifeTogetherTokens.spacing.small)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
        ) {
            AnimatedVisibility(
                visible = contentState?.currentGrocerySuggestions.orEmpty().isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
            ) {
                GrocerySuggestionPopup(
                    suggestions = contentState?.currentGrocerySuggestions.orEmpty(),
                    onClick = { suggestion ->
                        onUiEvent(GroceryListUiEvent.SuggestionClicked(suggestion))
                    },
                    modifier = Modifier.offset(y = 16.dp)
                )
            }
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

        if (showDeleteCompletedDialog) {
            ConfirmationDialog(
                onDismiss = { showDeleteCompletedDialog = false },
                onConfirm = {
                    showDeleteCompletedDialog = false
                    onUiEvent(GroceryListUiEvent.ConfirmDeleteCompletedConfirmation)
                },
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
private fun GroceryListScreenContentPreview() {
    LifeTogetherTheme {
        GroceryListScreen(
            uiState = GroceryListUiState.Content(
                groceryList = listOf(
                    GroceryItem(
                        id = "4",
                        familyId = "family-1",
                        category = UNCATEGORIZED_CATEGORY,
                        itemName = "Apple",
                        approxPrice = 12.0F
                    ),
                    GroceryItem(
                        id = "5",
                        familyId = "family-1",
                        category = UNCATEGORIZED_CATEGORY,
                        itemName = "Apple",
                    ),
                ),
                categorizedItems = mapOf(
                    UNCATEGORIZED_CATEGORY to listOf(
                        GroceryItem(
                            id = "1",
                            familyId = "family-1",
                            category = UNCATEGORIZED_CATEGORY,
                            itemName = "Apple",
                            approxPrice = 12.0F
                        ),
                        GroceryItem(
                            id = "2",
                            familyId = "family-1",
                            category = UNCATEGORIZED_CATEGORY,
                            itemName = "Banana",
                        ),
                    )
                ),
                completedItems = listOf(
                    GroceryItem(
                        id = "3",
                        familyId = "family-1",
                        category = UNCATEGORIZED_CATEGORY,
                        itemName = "Banana",
                    )
                ),
                groceryCategories = listOf(UNCATEGORIZED_CATEGORY),
                categoryExpandedStates = mapOf(
                    UNCATEGORIZED_CATEGORY_NAME to true,
                ),
                expectedTotalPrice = 40.0F,
                allGrocerySuggestions = emptyList(),
                currentGrocerySuggestions = listOf(GrocerySuggestion(
                    id = "294814r",
                    suggestionName = "Suggestion 1",
                    category = UNCATEGORIZED_CATEGORY,
                )),
                completedSectionExpanded = true,
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GroceryListScreenLoadingPreview() {
    LifeTogetherTheme {
        GroceryListScreen(
            uiState = GroceryListUiState.Loading,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
