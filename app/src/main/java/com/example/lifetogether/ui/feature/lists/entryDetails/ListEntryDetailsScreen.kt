package com.example.lifetogether.ui.feature.lists.entryDetails

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.tagOptionRow.TagOption
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListEntryDetailsScreen(
    uiState: EntryDetailsUiState,
    entryId: String? = null,
    familyId: String? = null,
    bitmap: Bitmap? = null,
    onImageUpload: suspend (Uri) -> Result<Unit, AppError> = { Result.Success(Unit) },
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
    val title = if (isExistingEntry) "Entry details" else "New entry"

    Scaffold(
        topBar = {
            AppTopBar(
                leftIcon = Icon(resId = R.drawable.ic_back_arrow, description = "back arrow"),
                onLeftClick = { onNavigationEvent(ListEntryDetailsNavigationEvent.NavigateBack) },
                text = title,
                rightIcon = if (isExistingEntry) Icon(resId = R.drawable.ic_edit, description = "edit entry") else null,
                onRightClick = if (isExistingEntry) {
                    if (content?.isEditing == true) {
                        { onUiEvent(ListEntryDetailsUiEvent.RequestCancelEdit) }
                    } else {
                        { onUiEvent(ListEntryDetailsUiEvent.EnterEditMode) }
                    }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .padding(LifeTogetherTokens.spacing.small),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                ) {
                    when (val details = contentState.details) {
                        is EntryDetailsContent.Routine -> routineEntryForm(
                            uiState = contentState,
                            isExistingEntry = isExistingEntry,
                            bitmap = bitmap,
                            pendingBitmap = details.form.pendingImageBitmap,
                            imagePickerLauncher = imagePickerLauncher,
                            formState = details.form,
                            onUiEvent = onUiEvent,
                        )

                        is EntryDetailsContent.Wish -> wishListEntryForm(
                            uiState = contentState,
                            formState = details.form,
                            onUiEvent = onUiEvent,
                        )

                        is EntryDetailsContent.Note -> notesEntryForm(
                            uiState = contentState,
                            formState = details.form,
                            onUiEvent = onUiEvent,
                        )

                        is EntryDetailsContent.Checklist -> checklistEntryForm(
                            uiState = contentState,
                            formState = details.form,
                            onUiEvent = onUiEvent,
                        )

                        is EntryDetailsContent.Meal -> mealPlannerEntryForm(
                            uiState = contentState,
                            formState = details.form,
                            onUiEvent = onUiEvent,
                        )
                    }
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

    if (content?.showImageUploadDialog == true && entryId != null && familyId != null) {
        ImageUploadDialog(
            onDismiss = { onUiEvent(ListEntryDetailsUiEvent.DismissImageUpload) },
            onConfirm = { onUiEvent(ListEntryDetailsUiEvent.ConfirmImageUpload) },
            onUpload = onImageUpload,
            dialogTitle = "Upload entry image",
            dialogMessage = "Select an image for this entry",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Upload image",
        )
    }
}

private fun LazyListScope.routineEntryForm(
    uiState: EntryDetailsUiState.Content,
    isExistingEntry: Boolean,
    bitmap: Bitmap?,
    pendingBitmap: Bitmap?,
    imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    formState: RoutineEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
    item {
        val displayBitmap = if (isExistingEntry) bitmap else pendingBitmap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.shapes.extraLarge,
                )
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
                TextDefault(
                    text = if (uiState.isEditing) "Tap to add image" else "No image",
                )
            }

            if (uiState.isEditing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(LifeTogetherTokens.spacing.small)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(horizontal = LifeTogetherTokens.spacing.small, vertical = LifeTogetherTokens.spacing.xSmall),
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
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
            TextSubHeadingMedium("Recurrence")
            TagOptionRow(
                options = RecurrenceUnit.entries.map { it.name.lowercase() },
                selectedOption = formState.recurrenceUnit.name.lowercase(),
                onSelectedOptionChange = {
                    if (uiState.isEditing) {
                        onUiEvent(ListEntryDetailsUiEvent.Routine.RecurrenceUnitChanged(it))
                    }
                },
                showDividers = false,
            )
        }
    }

    item {
        CustomTextField(
            value = formState.interval,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Routine.IntervalChanged(it)) },
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

            Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
            ) {
                ListEntryDetailsViewModel.WEEKDAYS.forEachIndexed { index, day ->
                    val dayNum = index + 1
                    TagOption(
                        tag = day,
                        selectedTag = if (dayNum in formState.selectedWeekdays) day else "",
                        onClick = {
                            if (uiState.isEditing) {
                                onUiEvent(ListEntryDetailsUiEvent.Routine.SelectedWeekdaysChanged(dayNum))
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun LazyListScope.wishListEntryForm(
    uiState: EntryDetailsUiState.Content,
    formState: WishEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
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
            value = formState.estimatedPriceMinor,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Wish.EstimatedPriceMinorChanged(it)) },
            label = "Estimated price (minor units)",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Number,
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
            enabled = uiState.isEditing,
        )
    }
}

private fun LazyListScope.notesEntryForm(
    uiState: EntryDetailsUiState.Content,
    formState: NoteEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)) {
            TextSubHeadingMedium("Mode")
            TagOptionRow(
                options = listOf("edit", "preview"),
                selectedOption = if (formState.isPreviewMode) "preview" else "edit",
                onSelectedOptionChange = {
                    onUiEvent(ListEntryDetailsUiEvent.Note.PreviewModeChanged(it == "preview"))
                },
                showDividers = false,
            )
        }
    }

    item {
        if (formState.isPreviewMode) {
            SelectionContainer {
                Text(
                    text = renderMarkdownPreview(formState.markdownBody),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        } else {
            CustomTextField(
                value = formState.markdownBody,
                onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Note.MarkdownBodyChanged(it)) },
                label = "Markdown body",
                modifier = Modifier.fillMaxWidth(),
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text,
                enabled = uiState.isEditing,
            )
        }
    }
}

private fun LazyListScope.checklistEntryForm(
    uiState: EntryDetailsUiState.Content,
    formState: ChecklistEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
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

private fun LazyListScope.mealPlannerEntryForm(
    uiState: EntryDetailsUiState.Content,
    formState: MealPlanEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
    item {
        CustomTextField(
            value = formState.date,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Meal.DateChanged(it)) },
            label = "Date (YYYY-MM-DD)",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = uiState.isEditing,
        )
    }
    item {
        CustomTextField(
            value = formState.recipeId,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Meal.RecipeIdChanged(it)) },
            label = "Recipe ID (optional)",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = uiState.isEditing,
        )
    }
    item {
        CustomTextField(
            value = formState.customMealName,
            onValueChange = { onUiEvent(ListEntryDetailsUiEvent.Meal.CustomMealNameChanged(it)) },
            label = "Custom meal name (optional)",
            modifier = Modifier.fillMaxWidth(),
            imeAction = ImeAction.Next,
            keyboardType = KeyboardType.Text,
            enabled = uiState.isEditing,
        )
    }
}

private fun renderMarkdownPreview(markdown: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = markdown.lines()
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.startsWith("# ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(line.removePrefix("# "))
                    pop()
                }

                line.startsWith("## ") -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(line.removePrefix("## "))
                    pop()
                }

                line.startsWith("- ") -> append("• ${line.removePrefix("- ")}")
                else -> append(line)
            }

            if (index != lines.lastIndex) append("\n")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryDetailsScreenPreview() {
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
                showImageUploadDialog = false,
            ),
            entryId = null,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
