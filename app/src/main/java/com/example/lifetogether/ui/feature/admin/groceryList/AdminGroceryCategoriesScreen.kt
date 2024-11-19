package com.example.lifetogether.ui.feature.admin.groceryList

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@Composable
fun AdminGroceryCategoriesScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
) {
    val groceryCategoriesViewModel: AdminGroceryCategoriesViewModel = hiltViewModel()

    val groceryCategories by groceryCategoriesViewModel.groceryCategories.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        groceryCategoriesViewModel.setUpCategories()
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
                TextHeadingMedium("Grocery categories")
                if (groceryCategories.isNotEmpty()) {
                    ListEditorContainer(
                        groceryCategories.map { category -> "${category.emoji} ${category.name}" },
                        onDelete = { categoryString ->
                            val categoryList = categoryString.split(" ", limit = 2)
                            groceryCategoriesViewModel.selectedCategory = Category(categoryList[0], categoryList[1])
                            groceryCategoriesViewModel.showDeleteCategoryConfirmationDialog = true
                        },
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Add new category as a string with an emoji and a name with whitespace between e.g. \"\uD83C\uDF5E Bakery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black,
                    )
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(shape = RoundedCornerShape(20))
                        .background(color = MaterialTheme.colorScheme.onBackground),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.8f),
                        ) {
                            CustomTextField(
                                value = groceryCategoriesViewModel.newCategory,
                                onValueChange = {
                                    groceryCategoriesViewModel.newCategory = it
                                },
                                label = "Add category",
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxHeight()
                                .clickable {
                                    groceryCategoriesViewModel.addCategory()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Add", color = Color.White)

                            Spacer(modifier = Modifier.width(5.dp))

                            Text(
                                text = ">",
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (groceryCategoriesViewModel.showDeleteCategoryConfirmationDialog && groceryCategoriesViewModel.selectedCategory != null) {
        ConfirmationDialog(
            onDismiss = { groceryCategoriesViewModel.showDeleteCategoryConfirmationDialog = false },
            onConfirm = {
                groceryCategoriesViewModel.deleteCategory()
            },
            dialogTitle = "Delete category?",
            dialogMessage = "Are you sure you want to delete the category: ${groceryCategoriesViewModel.selectedCategory!!.emoji} ${groceryCategoriesViewModel.selectedCategory!!.name}?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }

    if (groceryCategoriesViewModel.showAlertDialog) {
        ErrorAlertDialog(groceryCategoriesViewModel.error)
        groceryCategoriesViewModel.toggleAlertDialog()
    }
}

@Preview(showBackground = true)
@Composable
fun AdminGroceryListScreenPreview() {
    LifeTogetherTheme {
        AdminGroceryCategoriesScreen()
    }
}
