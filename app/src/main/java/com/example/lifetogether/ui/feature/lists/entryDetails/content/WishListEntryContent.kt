package com.example.lifetogether.ui.feature.lists.entryDetails.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsContent
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsUiState
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsUiEvent
import com.example.lifetogether.ui.feature.lists.entryDetails.WishEntryFormState
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

fun LazyListScope.wishListEntryForm(
    uiState: EntryDetailsUiState.Content,
    formState: WishEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
    item {
        CustomTextField(
            value = formState.name,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.NameChanged(it)) },
            label = "Name",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            capitalization = true,
            enabled = uiState.isEditing,
        )
    }
    item {
        CustomTextField(
            value = formState.url,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Wish.UrlChanged(it)) },
            label = "URL",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Uri,
            enabled = uiState.isEditing,
        )
    }
    item {
        CustomTextField(
            value = formState.price,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Wish.PriceChanged(it)) },
            label = "Price",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Decimal,
            enabled = uiState.isEditing,
        )
    }
    item {
        CustomTextField(
            value = formState.currencyCode,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Wish.CurrencyCodeChanged(it)) },
            label = "Currency code",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = uiState.isEditing,
        )
    }
    item {
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
            TextSubHeadingMedium("Priority")
            TagOptionRow(
                options = listOf("urgent", "planned", "someday"),
                selectedOption = formState.priority.value,
                onSelectedOptionChange = {
                    if (uiState.isEditing) onUiEvent(ListEntryDetailsUiEvent.Wish.PriorityChanged(it))
                },
                showDividers = false,
            )
        }
    }
    item {
        CustomTextField(
            value = formState.notes,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Wish.NotesChanged(it)) },
            label = "Notes",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            capitalization = true,
            enabled = uiState.isEditing,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WishListEntryContentPreview() {
    LifeTogetherTheme {
        val uiState = EntryDetailsUiState.Content(
            details = EntryDetailsContent.Wish.blank(),
            isEditing = true
        )
        ListEntryDetailsContent(
            padding = PaddingValues(0.dp),
            contentState = uiState,
            isExistingEntry = false,
            onUiEvent = {},
        ) {
            wishListEntryForm(
                uiState = uiState,
                formState = WishEntryFormState(
                    name = "Camping stove",
                    url = "https://example.com/stove",
                    price = "79.99",
                    currencyCode = "USD",
                ),
                onUiEvent = {},
            )
        }
    }
}