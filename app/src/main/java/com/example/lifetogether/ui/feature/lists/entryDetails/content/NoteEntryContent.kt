package com.example.lifetogether.ui.feature.lists.entryDetails.content

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.textfield.MarkdownVisualTransformation
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsContent
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsUiState
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsUiEvent
import com.example.lifetogether.ui.feature.lists.entryDetails.NoteEntryFormState
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun NoteEntryContent(
    uiState: EntryDetailsUiState.Content,
    formState: NoteEntryFormState,
    isExistingEntry: Boolean,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
    val scrollState = rememberScrollState()
    val titleFocusRequester = remember { FocusRequester() }
    val bodyFocusRequester = remember { FocusRequester() }
    val screenHeight = LocalWindowInfo.current.containerSize.height.dp
    val bodyMinHeight = screenHeight * 0.6f

    LaunchedEffect(Unit) {
        if (uiState.isEditing && !isExistingEntry) {
            titleFocusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = LifeTogetherTokens.spacing.medium)
            .padding(top = LifeTogetherTokens.spacing.medium),
    ) {
        BasicTextField( //todo this is never shown when creating a new note
            value = formState.name,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.NameChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(titleFocusRequester),
            enabled = uiState.isEditing,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                if (formState.name.isEmpty()) {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                innerTextField()
            },
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = LifeTogetherTokens.spacing.small),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        BasicTextField(
            value = formState.body,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Note.BodyChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = bodyMinHeight)
                .focusRequester(bodyFocusRequester)
                .then(
                    if (!uiState.isEditing) {
                        Modifier.clickable { onUiEvent(ListEntryDetailsUiEvent.EnterEditMode) }
                    } else {
                        Modifier
                    },
                ),
            enabled = uiState.isEditing,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = MarkdownVisualTransformation(),
            decorationBox = { innerTextField ->
                if (formState.body.isEmpty()) {
                    Text(
                        text = "Start writing…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
                innerTextField()
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteEntryContentEditPreview() {
    LifeTogetherTheme {
        NoteEntryContent(
            uiState = EntryDetailsUiState.Content(
                details = EntryDetailsContent.Note(form = NoteEntryFormState()),
                isEditing = true,
            ),
            formState = NoteEntryFormState(
                name = "Shopping notes",
                body = "# Things to buy\n- Milk\n- Eggs\n## Later\nCheck the cupboard first",
            ),
            isExistingEntry = false,
            onUiEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteEntryContentViewPreview() {
    LifeTogetherTheme {
        NoteEntryContent(
            uiState = EntryDetailsUiState.Content(
                details = EntryDetailsContent.Note(form = NoteEntryFormState()),
                isEditing = false,
            ),
            formState = NoteEntryFormState(
                name = "Shopping notes",
                body = "# Things to buy\n- Milk\n- Eggs\n## Later\nCheck the cupboard first",
            ),
            isExistingEntry = true,
            onUiEvent = {},
        )
    }
}
