package com.example.lifetogether.ui.feature.lists.entryDetails

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.tagOptionRow.TagOption
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEntryDetailsScreen(
    screenState: EntryDetailsScreenState,
    entryId: String? = null,
    familyId: String? = null,
    bitmap: Bitmap? = null,
    showImageUploadDialog: Boolean = false,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
    onNavigationEvent: (ListEntryDetailsNavigationEvent) -> Unit,
) {
    val uiState = screenState.uiState
    val formState = screenState.formState
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { onUiEvent(ListEntryDetailsUiEvent.ImageSelected(it)) }
    }

    val isExistingEntry = entryId != null
    val title = if (isExistingEntry) "Entry details" else "New entry"

    Scaffold(
        topBar = {
            TopBar(
                leftIcon = Icon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                onLeftClick = { onNavigationEvent(ListEntryDetailsNavigationEvent.NavigateBack) },
                text = title,
                rightIcon = if (isExistingEntry) Icon(resId = R.drawable.ic_edit, description = "edit entry") else null,
                onRightClick = if (isExistingEntry) {
                    if ((uiState as? EntryDetailsUiState.Content)?.isEditing == true) {
                        { onUiEvent(ListEntryDetailsUiEvent.RequestCancelEdit) }
                    } else {
                        { onUiEvent(ListEntryDetailsUiEvent.EnterEditMode) }
                    }
                } else null,
            )
        }
    ) { padding ->
        when (uiState) {
            is EntryDetailsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding()),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is EntryDetailsUiState.Content -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                        .padding(10.dp),
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            val displayBitmap = if (isExistingEntry) bitmap else formState.pendingImageBitmap
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = uiState.isEditing) {
                                        if (isExistingEntry) {
                                            onUiEvent(ListEntryDetailsUiEvent.RequestImageUpload)
                                        } else {
                                            imagePickerLauncher.launch("image/*")
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (displayBitmap != null) {
                                    Image(
                                        modifier = Modifier.fillMaxSize(),
                                        bitmap = displayBitmap.asImageBitmap(),
                                        contentDescription = "entry image",
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(
                                        text = if (uiState.isEditing) "Tap to add image" else "No image",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }

                                if (uiState.isEditing) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                                shape = MaterialTheme.shapes.small,
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = if (displayBitmap != null) "Change image" else "Add image",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }

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
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextSubHeadingMedium("Recurrence")
                                TagOptionRow(
                                    options = RecurrenceUnit.entries.map { it.name.lowercase() },
                                    selectedOption = formState.recurrenceUnit.name,
                                    onSelectedOptionChange = {
                                        if (uiState.isEditing) {
                                            onUiEvent(ListEntryDetailsUiEvent.RecurrenceUnitChanged(it))
                                        }
                                    },
                                    showDividers = false,
                                )
                            }
                        }

                        item {
                            CustomTextField(
                                value = formState.interval,
                                onValueChange = { onUiEvent(ListEntryDetailsUiEvent.IntervalChanged(it)) },
                                label = "Interval (N)",
                                modifier = Modifier.fillMaxWidth(),
                                imeAction = ImeAction.Next,
                                keyboardType = KeyboardType.Number,
                                enabled = uiState.isEditing,
                            )
                        }

                        if (formState.recurrenceUnit == RecurrenceUnit.WEEKS) {
                            item {
                                TextSubHeadingMedium("Weekdays")

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    ListEntryDetailsViewModel.WEEKDAYS.forEachIndexed { index, day ->
                                        val dayNum = index + 1
                                        TagOption(
                                            tag = day,
                                            selectedTag = if (dayNum in formState.selectedWeekdays) day else "",
                                            onClick = {
                                                if (uiState.isEditing) {
                                                    onUiEvent(ListEntryDetailsUiEvent.SelectedWeekdaysChanged(dayNum))
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.isEditing) {
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .padding(bottom = padding.calculateBottomPadding())
                                .align(Alignment.End),
                        ) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onUiEvent(ListEntryDetailsUiEvent.SaveClicked) },
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator()
                                } else {
                                    Text(if (isExistingEntry) "Save changes" else "Create")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState is EntryDetailsUiState.Content && uiState.showDiscardDialog) {
        ConfirmationDialog(
            onDismiss = { onUiEvent(ListEntryDetailsUiEvent.DismissDiscardDialog) },
            onConfirm = { onUiEvent(ListEntryDetailsUiEvent.ConfirmDiscard) },
            dialogTitle = "Discard changes?",
            dialogMessage = "Your unsaved changes will be lost.",
            dismissButtonMessage = "Keep editing",
            confirmButtonMessage = "Discard",
        )
    }

    if (showImageUploadDialog && entryId != null && familyId != null) {
        ImageUploadDialog(
            onDismiss = { onUiEvent(ListEntryDetailsUiEvent.DismissImageUpload) },
            onConfirm = { onUiEvent(ListEntryDetailsUiEvent.ConfirmImageUpload) },
            dialogTitle = "Upload entry image",
            dialogMessage = "Select an image for this entry",
            imageType = ImageType.RoutineListEntryImage(familyId, entryId),
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Upload image",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryDetailsScreenPreview() {
    LifeTogetherTheme {
        ListEntryDetailsScreen(
            screenState = EntryDetailsScreenState(
                uiState = EntryDetailsUiState.Content(isEditing = true),
                formState = EntryFormState(
                    name = "Water the plants",
                    recurrenceUnit = RecurrenceUnit.WEEKS,
                    interval = "2",
                    selectedWeekdays = setOf(1, 4),
                ),
            ),
            entryId = null,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
