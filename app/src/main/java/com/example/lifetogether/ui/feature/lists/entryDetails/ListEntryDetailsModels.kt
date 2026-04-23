package com.example.lifetogether.ui.feature.lists.entryDetails

import android.graphics.Bitmap
import android.net.Uri
import com.example.lifetogether.domain.model.lists.RecurrenceUnit

sealed interface EntryDetailsUiState {
    data object Loading : EntryDetailsUiState
    data class Content(
        val isEditing: Boolean = false,
        val showDiscardDialog: Boolean = false,
        val isSaving: Boolean = false,
    ) : EntryDetailsUiState
}

data class EntryFormState(
    val name: String = "",
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
    val interval: String = "1",
    val selectedWeekdays: Set<Int> = emptySet(),
    val pendingImageUri: Uri? = null,
    val pendingImageBitmap: Bitmap? = null,
)

data class EntryDetailsScreenState(
    val uiState: EntryDetailsUiState = EntryDetailsUiState.Loading,
    val formState: EntryFormState = EntryFormState(),
)

sealed interface ListEntryDetailsUiEvent {
    data object EnterEditMode : ListEntryDetailsUiEvent
    data object RequestCancelEdit : ListEntryDetailsUiEvent
    data object ConfirmDiscard : ListEntryDetailsUiEvent
    data object DismissDiscardDialog : ListEntryDetailsUiEvent
    data class NameChanged(val value: String) : ListEntryDetailsUiEvent
    data class RecurrenceUnitChanged(val value: String) : ListEntryDetailsUiEvent
    data class IntervalChanged(val value: String) : ListEntryDetailsUiEvent
    data class SelectedWeekdaysChanged(val dayNum: Int) : ListEntryDetailsUiEvent
    data object SaveClicked : ListEntryDetailsUiEvent
    data class ImageSelected(val uri: Uri) : ListEntryDetailsUiEvent
    data object RequestImageUpload : ListEntryDetailsUiEvent
    data object DismissImageUpload : ListEntryDetailsUiEvent
    data object ConfirmImageUpload : ListEntryDetailsUiEvent
}

sealed interface ListEntryDetailsNavigationEvent {
    data object NavigateBack : ListEntryDetailsNavigationEvent
}
