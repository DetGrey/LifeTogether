package com.example.lifetogether.ui.feature.lists.entryDetails.content

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.ui.common.tagOptionRow.TagOptionRow
import com.example.lifetogether.ui.common.tagOptionRow.TagOption
import com.example.lifetogether.ui.common.image.AnimatedBitmapImage
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsContent
import com.example.lifetogether.ui.feature.lists.entryDetails.EntryDetailsUiState
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsUiEvent
import com.example.lifetogether.ui.feature.lists.entryDetails.ListEntryDetailsViewModel
import com.example.lifetogether.ui.feature.lists.entryDetails.RoutineEntryFormState
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

fun LazyListScope.routineEntryForm(
    uiState: EntryDetailsUiState.Content,
    bitmap: Bitmap?,
    pendingBitmap: Bitmap?,
    onLaunchImagePicker: ((String) -> Unit)? = null,
    formState: RoutineEntryFormState,
    onUiEvent: (ListEntryDetailsUiEvent) -> Unit,
) {
    item {
        val displayBitmap = pendingBitmap ?: bitmap
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.shapes.extraLarge,
                )
                .clickable(enabled = uiState.isEditing) {
                    onLaunchImagePicker?.invoke("image/*")
                },
            contentAlignment = Alignment.Center,
        ) {
            if (displayBitmap == null) {
                TextDefault(
                    text = if (uiState.isEditing) "Tap to add image" else "No image",
                )
            }
            AnimatedBitmapImage(
                bitmap = displayBitmap,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "entry image",
            )

            if (uiState.isEditing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(LifeTogetherTokens.spacing.small)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(
                            horizontal = LifeTogetherTokens.spacing.small,
                            vertical = LifeTogetherTokens.spacing.xSmall,
                        ),
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

@Preview(showBackground = true)
@Composable
private fun RoutineEntryContentPreview() {
    LifeTogetherTheme {
        val uiState = EntryDetailsUiState.Content(
            details = EntryDetailsContent.Routine.blank(),
            isEditing = true
        )
        ListEntryDetailsContent(
            padding = PaddingValues(0.dp),
            contentState = uiState,
            isExistingEntry = false,
            onUiEvent = {},
        ) {
            routineEntryForm(
                uiState = uiState,
                bitmap = null,
                pendingBitmap = null,
                onLaunchImagePicker = null,
                formState = RoutineEntryFormState(
                    name = "Water the plants",
                    recurrenceUnit = RecurrenceUnit.WEEKS,
                    interval = "2",
                    selectedWeekdays = setOf(1, 4),
                ),
                onUiEvent = {},
            )
        }
    }
}
