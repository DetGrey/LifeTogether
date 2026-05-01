package com.example.lifetogether.ui.feature.admin.groceryList.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun AdminGroceryCategoriesScreen(
    uiState: AdminGroceryCategoriesUiState,
    onUiEvent: (AdminGroceryCategoriesUiEvent) -> Unit,
    onNavigationEvent: (AdminGroceryCategoriesNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(AdminGroceryCategoriesNavigationEvent.NavigateBack)
                },
                text = "Edit grocery list",
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(LifeTogetherTokens.spacing.small)
                .padding(bottom = LifeTogetherTokens.spacing.bottomInsetMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
        ) {

            item {
                Text(
                    modifier = Modifier.padding(horizontal = LifeTogetherTokens.spacing.xSmall),
                    text = "Add new category as a string with an emoji and a name with whitespace between e.g. \"\uD83C\uDF5E Bakery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))

                TextHeadingMedium("Grocery categories")

                if (uiState.groceryCategories.isNotEmpty()) {
                    ListEditorContainer(
                        uiState.groceryCategories.map { category -> "${category.emoji} ${category.name}" },
                        onDelete = { categoryString ->
                            val categoryList = categoryString.split(" ", limit = 2)
                            onUiEvent(
                                AdminGroceryCategoriesUiEvent.DeleteCategoryClicked(
                                    Category(categoryList[0], categoryList[1]),
                                ),
                            )
                        },
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(LifeTogetherTokens.spacing.small),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(shape = RoundedCornerShape(LifeTogetherTokens.spacing.large))
                    .background(color = MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.8f),
                    ) {
                        CustomTextField(
                            value = uiState.newCategory,
                            onValueChange = {
                                onUiEvent(AdminGroceryCategoriesUiEvent.NewCategoryChanged(it))
                            },
                            label = "Add category",
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .padding(LifeTogetherTokens.spacing.small)
                            .fillMaxHeight()
                            .clickable {
                                onUiEvent(AdminGroceryCategoriesUiEvent.AddCategoryClicked)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Add", color = MaterialTheme.colorScheme.secondary)

                        Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.xSmall))

                        Text(
                            text = ">",
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }
        }

        if (uiState.showDeleteCategoryConfirmationDialog && uiState.selectedCategory != null) {
            ConfirmationDialog(
                onDismiss = { onUiEvent(AdminGroceryCategoriesUiEvent.DismissDeleteCategoryConfirmation) },
                onConfirm = { onUiEvent(AdminGroceryCategoriesUiEvent.ConfirmDeleteCategory) },
                dialogTitle = "Delete category?",
                dialogMessage = "Are you sure you want to delete the category: ${uiState.selectedCategory.emoji} ${uiState.selectedCategory.name}?",
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Delete",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminGroceryCategoriesScreenPreview() {
    LifeTogetherTheme {
        AdminGroceryCategoriesScreen(
            uiState = AdminGroceryCategoriesUiState(
                groceryCategories = listOf(
                    Category("❓️", "Uncategorized"),
                    Category("🥦", "Vegetables"),
                ),
                newCategory = "❓Frozen"
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
