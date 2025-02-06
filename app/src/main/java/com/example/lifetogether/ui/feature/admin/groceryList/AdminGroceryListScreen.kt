package com.example.lifetogether.ui.feature.admin.groceryList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.CustomTextField
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@Composable
fun AdminGroceryListScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    list: List<String>? = null,
) {
    val groceryListViewModel: AdminGroceryListViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val groceryCategories by groceryListViewModel.groceryCategories.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        groceryListViewModel.setUpGroceryList()
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
                if (groceryCategories.isNotEmpty()) {
                    TextHeadingMedium("Grocery categories")
                    if (list != null) {
                        ListEditorContainer(list) { }
                    }
                    // for (category in groceryCategories) {
                    // TextDefault("${category.emoji} ${category.name}")
                    // }
                    CustomTextField(
                        value = "",
                        onValueChange = {},
                        label = "Add category",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    )
                }
            }

            item {
            }
        }
    }

    if (groceryListViewModel.showConfirmationDialog) {
        ConfirmationDialog(
            onDismiss = { groceryListViewModel.showConfirmationDialog = false },
            onConfirm = {
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
fun AdminGroceryListScreenPreview() {
    LifeTogetherTheme {
        AdminGroceryListScreen(list = listOf("🍎 Fruits and vegetables", "🍞 Bakery", "❄️ Frozen food"))
    }
}
