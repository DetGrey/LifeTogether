package com.example.lifetogether.ui.feature.lists.entryDetails.content

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsUiState
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsUiEvent
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ListEntryDetailsContent(
    padding: PaddingValues,
    contentState: EntryDetailsUiState.Content,
    isExistingEntry: Boolean,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
    content: LazyListScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = padding.calculateTopPadding())
            .padding(LifeTogetherTokens.spacing.small),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.large),
        ) {
            content()
        }
        AnimatedVisibility(
            visible = contentState.isEditing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .padding(bottom = padding.calculateBottomPadding())
                    .align(Alignment.End),
            ) {
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = if (isExistingEntry) "Save changes" else "Create",
                    onClick = { onUiEvent(ListEntryDetailsUiEvent.SaveClicked) },
                    loading = contentState.isSaving,
                )
            }
        }
    }
}