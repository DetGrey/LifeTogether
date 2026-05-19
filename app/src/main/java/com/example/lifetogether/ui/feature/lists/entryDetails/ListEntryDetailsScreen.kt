package com.example.lifetogether.ui.feature.lists.entryDetails

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.feature.lists.entryDetails.content.ListEntryDetailsContent
import com.example.lifetogether.ui.feature.lists.entryDetails.content.NoteEntryContent
import com.example.lifetogether.ui.feature.lists.entryDetails.content.routineEntryForm
import com.example.lifetogether.ui.feature.lists.entryDetails.content.wishListEntryForm
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEntryDetailsScreen(
    uiState: EntryDetailsUiState,
    entryId: String? = null,
    bitmap: Bitmap? = null,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
    onNavigationEvent: (ListEntryDetailsNavigationEvent) -> Unit,
) {
    val content = uiState as? EntryDetailsUiState.Content
    val isLoading = uiState is EntryDetailsUiState.Loading
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { onUiEvent(ListEntryDetailsUiEvent.Routine.ImageSelected(it)) }
    }

    val isExistingEntry = entryId != null
    val isNoteEntry = content?.details is EntryDetailsContent.Note

    val topBarTitle = if (isNoteEntry) "" else if (isExistingEntry) "Entry details" else "New entry"
    val topBarRightAppIcon = when {
        isNoteEntry || !isExistingEntry -> null
        else -> AppIcon(resId = R.drawable.ic_edit, description = "edit entry")
    }
    val topBarRightClick: (() -> Unit)? = if (isExistingEntry && !isNoteEntry) {
        when {
            content?.isEditing == true -> { { onUiEvent(ListEntryDetailsUiEvent.RequestCancelEdit) } }
            else -> { { onUiEvent(ListEntryDetailsUiEvent.EnterEditMode) } }
        }
    } else null
    val topBarRightText = if (isNoteEntry && content.isEditing) "Done" else null

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                onLeftClick = { onNavigationEvent(ListEntryDetailsNavigationEvent.NavigateBack) },
                text = topBarTitle,
                rightAppIcon = topBarRightAppIcon,
                onRightClick = topBarRightClick,
                rightText = topBarRightText,
                onRightTextClick = if (topBarRightText != null) {
                    { onUiEvent(ListEntryDetailsUiEvent.SaveClicked) }
                } else null,
            )
        }
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = isLoading,
            label = "entry_details_loading_content",
            loadingContent = {
                Skeletons.FormEdit(modifier = Modifier.fillMaxSize())
            },
        ) {
            val contentState = content ?: return@AnimatedLoadingContent

            when (val details = contentState.details) {
                is EntryDetailsContent.Note -> NoteEntryContent(
                    padding = padding,
                    uiState = contentState,
                    formState = details.form,
                    isExistingEntry = isExistingEntry,
                    onUiEvent = onUiEvent,
                )

                else -> ListEntryDetailsContent(
                    padding = padding,
                    contentState = contentState,
                    isExistingEntry = isExistingEntry,
                    onUiEvent = onUiEvent,
                ) {
                    when (details) {
                        is EntryDetailsContent.Routine -> routineEntryForm(
                            uiState = contentState,
                            bitmap = bitmap,
                            pendingBitmap = details.form.pendingImageBitmap,
                            onLaunchImagePicker = { imagePickerLauncher.launch(it) },
                            formState = details.form,
                            onUiEvent = onUiEvent,
                        )

                        is EntryDetailsContent.Wish -> wishListEntryForm(
                            uiState = contentState,
                            formState = details.form,
                            onUiEvent = onUiEvent,
                        )

                        is EntryDetailsContent.Note -> Unit
                    }
                }
            }
        }
    }

    if (content?.showDiscardDialog == true) {
        ConfirmationDialog(
            onDismiss = { onUiEvent(ListEntryDetailsUiEvent.DismissDiscardDialog) },
            onConfirm = { onUiEvent(ListEntryDetailsUiEvent.ConfirmDiscard) },
            dialogTitle = "Discard changes?",
            dialogMessage = "Your unsaved changes will be lost.",
            dismissButtonMessage = "Keep editing",
            confirmButtonMessage = "Discard",
        )
    }

}

@Preview(showBackground = true)
@Composable
private fun ListEntryDetailsScreenPreview() {
    LifeTogetherTheme {
        ListEntryDetailsScreen(
            uiState = EntryDetailsUiState.Content(
                details = EntryDetailsContent.Routine.blank().copy(
                    form = RoutineEntryFormState(
                        name = "Water the plants",
                        recurrenceUnit = RecurrenceUnit.WEEKS,
                        interval = "2",
                        selectedWeekdays = setOf(1, 4),
                    ),
                ),
                isEditing = true,
                showDiscardDialog = false,
                isSaving = false,
            ),
            entryId = null,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
