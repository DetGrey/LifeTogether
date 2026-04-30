package com.example.lifetogether.ui.common.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithDropdown
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY

@Composable
fun AddNewListItem(
    textValue: String,
    onTextChange: (String) -> Unit,
    priceValue: String,
    onPriceChange: (String) -> Unit,
    onAddClick: () -> Unit,
    categoryList: List<Category>,
    selectedCategory: Category,
    onCategoryChange: (Category) -> Unit,
) {
    ListItemInputBar(
        textValue = textValue,
        onTextChange = onTextChange,
        priceValue = priceValue,
        onPriceChange = onPriceChange,
        onActionClick = onAddClick,
        categoryList = categoryList,
        selectedCategory = selectedCategory,
        onCategoryChange = onCategoryChange,
        textFieldLabel = "Add item...",
        actionLabel = "Add",
    )
}

@Composable
fun EditListItem(
    textValue: String,
    onTextChange: (String) -> Unit,
    priceValue: String,
    onPriceChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    categoryList: List<Category>,
    selectedCategory: Category,
    onCategoryChange: (Category) -> Unit,
) {
    ListItemInputBar(
        textValue = textValue,
        onTextChange = onTextChange,
        priceValue = priceValue,
        onPriceChange = onPriceChange,
        onActionClick = onSaveClick,
        categoryList = categoryList,
        selectedCategory = selectedCategory,
        onCategoryChange = onCategoryChange,
        textFieldLabel = "Edit item...",
        actionLabel = "Save",
    )
}

@Composable
private fun ListItemInputBar(
    textValue: String,
    onTextChange: (String) -> Unit,
    priceValue: String,
    onPriceChange: (String) -> Unit,
    onActionClick: () -> Unit,
    categoryList: List<Category>,
    selectedCategory: Category,
    onCategoryChange: (Category) -> Unit,
    textFieldLabel: String,
    actionLabel: String,
) {
    var showDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedDialogCategory by remember(selectedCategory, categoryList) {
        mutableStateOf(
            categoryList.firstOrNull {
                (it.emoji == selectedCategory.emoji && it.name == selectedCategory.name)
            } ?: selectedCategory,
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeTogetherTokens.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .aspectRatio(1f)
                    .clickable {
                        selectedDialogCategory = selectedCategory
                        showDialog = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = selectedCategory.emoji,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            CustomTextField(
                modifier = Modifier
                    .weight(1f),
                value = textValue,
                onValueChange = onTextChange,
                label = textFieldLabel,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
                capitalization = true,
            )

            CustomTextField(
                modifier = Modifier.weight(0.65f),
                value = priceValue,
                onValueChange = onPriceChange,
                label = "Price",
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                smaller = true,
            )

            Row(
                modifier = Modifier
                    .clickable { onActionClick() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = actionLabel,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.xSmall))

                Text(
                    text = ">",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }

    if (showDialog) {
        ConfirmationDialogWithDropdown(
            onDismiss = {
                expanded = false
                showDialog = false
            },
            onConfirm = {
                onCategoryChange(selectedDialogCategory)
                expanded = false
                showDialog = false
            },
            dialogTitle = "Change category",
            dialogMessage = "Choose a category from the dropdown below",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Change",
            selectedValue = selectedDialogCategory,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            options = categoryList,
            onValueChange = { selectedDialogCategory = it },
            optionLabel = { "${it.emoji} ${it.name}" },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    LifeTogetherTheme {
        Column {
            AddNewListItem(
                textValue = "Apple",
                onTextChange = { },
                priceValue = "1.5",
                onPriceChange = { },
                onAddClick = { },
                categoryList = listOf(),
                selectedCategory = UNCATEGORIZED_CATEGORY,
                onCategoryChange = { },
            )
            EditListItem(
                textValue = "Banana",
                onTextChange = { },
                priceValue = "",
                onPriceChange = { },
                onSaveClick = { },
                categoryList = listOf(),
                selectedCategory = UNCATEGORIZED_CATEGORY,
                onCategoryChange = { },
            )
        }
    }
}
