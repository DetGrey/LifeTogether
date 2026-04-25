package com.example.lifetogether.ui.common.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithDropdown
import com.example.lifetogether.ui.common.textfield.CustomTextField

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
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f, true)
                        .clip(shape = CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                        )
                        .clickable {
                            selectedDialogCategory = selectedCategory
                            showDialog = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = selectedCategory.emoji)
                }

                CustomTextField(
                    value = textValue,
                    onValueChange = onTextChange,
                    label = textFieldLabel,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    capitalization = true,
                )

                CustomTextField(
                    value = priceValue,
                    onValueChange = onPriceChange,
                    label = "Price",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    smaller = true,
                )
            }

            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxHeight()
                    .clickable {
                        onActionClick()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = actionLabel, color = Color.White)

                Spacer(modifier = Modifier.width(5.dp))

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
            dialogMessage = "",
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
