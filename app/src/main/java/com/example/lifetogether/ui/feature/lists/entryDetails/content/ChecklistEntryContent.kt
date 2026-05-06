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
import com.example.lifetogether.ui.feature.lists.entryDetails.ChecklistEntryFormState
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsContent
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsUiState
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsUiEvent
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

fun LazyListScope.checklistEntryForm( //todo remove. Should not even exist since it should just be like the grocery list
    uiState: EntryDetailsUiState.Content,
    formState: ChecklistEntryFormState,
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
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
            TextSubHeadingMedium("Status")
            TagOptionRow(
                options = listOf("pending", "completed"),
                selectedOption = if (formState.isChecked) "completed" else "pending",
                onSelectedOptionChange = {
                    if (uiState.isEditing) {
                        onUiEvent(ListEntryDetailsUiEvent.Checklist.CheckedChanged(it == "completed"))
                    }
                },
                showDividers = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChecklistEntryContentPreview() {
    LifeTogetherTheme {
        val uiState = EntryDetailsUiState.Content(
            details = EntryDetailsContent.Meal.blank(),
            isEditing = true
        )
        ListEntryDetailsContent(
            padding = PaddingValues(0.dp),
            contentState = uiState,
            isExistingEntry = false,
            onUiEvent = {},
        ) {
            checklistEntryForm(
                uiState = uiState,
                formState = ChecklistEntryFormState(
                    name = "Packing list",
                    isChecked = false,
                ),
                onUiEvent = {},
            )
        }
    }
}