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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithDropdown
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTokens

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
    val addNewListItemViewModel: AddNewListItemViewModel = hiltViewModel()

    LaunchedEffect(key1 = "init") {
        addNewListItemViewModel.selectedCategory = "${selectedCategory.emoji} ${selectedCategory.name}"
    }

    if (categoryList != addNewListItemViewModel.oldCategoryList) {
        addNewListItemViewModel.oldCategoryList = categoryList
        addNewListItemViewModel.categoryOptions = categoryList.map { "${it.emoji} ${it.name}" }
        println("AddNewListItem categoryOptions: ${addNewListItemViewModel.categoryOptions}")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape = MaterialTheme.shapes.large)
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
                        .padding(LifeTogetherTokens.spacing.small)
                        .fillMaxHeight()
                        .aspectRatio(1f, true)
                        .clip(shape = CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                        )
                        .clickable {
                            addNewListItemViewModel.showDialog = true
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
                    .padding(LifeTogetherTokens.spacing.small)
                    .fillMaxHeight()
                    .clickable {
                        onActionClick()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = actionLabel, color = MaterialTheme.colorScheme.onBackground)

                Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.xSmall))

                Text(
                    text = ">",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }

    if (addNewListItemViewModel.showDialog) {
        ConfirmationDialogWithDropdown(
            onDismiss = { addNewListItemViewModel.showDialog = false },
            onConfirm = {
                val string = addNewListItemViewModel.selectedCategory
                var emojiEndIndex = string.offsetByCodePoints(0, 1)
                var emoji = string.substring(0, emojiEndIndex)

                var category = categoryList.find { it.emoji == emoji }

                if (category == null) {
                    emojiEndIndex = string.offsetByCodePoints(0, string.codePointCount(0, string.length).coerceAtMost(2))
                    emoji = string.substring(0, emojiEndIndex)
                    category = categoryList.find { it.emoji == emoji }
                }

                val name = string.substring(emojiEndIndex)
                println("category list: [$emoji, $name]")

                if (category != null) {
                    onCategoryChange(category)
                }
                addNewListItemViewModel.showDialog = false
            },
            dialogTitle = "Change category",
            dialogMessage = "",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Change",
            selectedValue = addNewListItemViewModel.selectedCategory,
            expanded = addNewListItemViewModel.changeCategoryExpanded,
            onExpandedChange = {
                addNewListItemViewModel.changeCategoryExpanded =
                    !addNewListItemViewModel.changeCategoryExpanded
            },
            options = addNewListItemViewModel.categoryOptions,
            onValueChange = { string ->
                addNewListItemViewModel.selectedCategory = string
            },
        )
    }
}
