package com.example.lifetogether.ui.feature.lists.entryDetails

import android.graphics.Bitmap
import android.net.Uri
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.lists.WishListPriority
import java.util.Date

sealed interface EntryDetailsUiState {
    data object Loading : EntryDetailsUiState

    data class Content(
        val details: EntryDetailsContent,
        val isEditing: Boolean = false,
        val showDiscardDialog: Boolean = false,
        val isSaving: Boolean = false,
    ) : EntryDetailsUiState
}

sealed interface EntryDetailsContent {
    val listType: ListType

    data class Routine(
        val form: RoutineEntryFormState,
    ) : EntryDetailsContent {
        override val listType: ListType = ListType.ROUTINE

        companion object {
            fun blank(): Routine {
                return Routine(form = RoutineEntryFormState())
            }

            fun from(entry: RoutineListEntry): Routine {
                return Routine(
                    form = RoutineEntryFormState(
                        name = entry.itemName,
                        recurrenceUnit = entry.recurrenceUnit,
                        interval = entry.interval.toString(),
                        selectedWeekdays = entry.weekdays.toSet(),
                        dateCreated = entry.dateCreated,
                        lastCompletedAt = entry.lastCompletedAt,
                        completionCount = entry.completionCount,
                        imageUrl = entry.imageUrl,
                    ),
                )
            }
        }
    }

    data class Wish(
        val form: WishEntryFormState,
    ) : EntryDetailsContent {
        override val listType: ListType = ListType.WISH_LIST

        companion object {
            fun blank(): Wish {
                return Wish(form = WishEntryFormState())
            }

            fun from(entry: WishListEntry): Wish {
                return Wish(
                    form = WishEntryFormState(
                        name = entry.itemName,
                        purchased = entry.purchased,
                        url = entry.url.orEmpty(),
                        price = entry.price?.toString().orEmpty(),
                        currencyCode = entry.currencyCode.orEmpty(),
                        priority = entry.priority,
                        notes = entry.notes.orEmpty(),
                    ),
                )
            }
        }
    }

    data class Note(
        val form: NoteEntryFormState,
    ) : EntryDetailsContent {
        override val listType: ListType = ListType.NOTES

        companion object {
            fun blank(): Note {
                return Note(form = NoteEntryFormState())
            }

            fun from(entry: NoteEntry): Note {
                return Note(
                    form = NoteEntryFormState(
                        name = entry.itemName,
                        body = entry.body,
                    ),
                )
            }
        }
    }

}

data class RoutineEntryFormState(
    val name: String = "",
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
    val interval: String = "1",
    val selectedWeekdays: Set<Int> = emptySet(),
    val pendingImageUri: Uri? = null,
    val pendingImageBitmap: Bitmap? = null,
    val dateCreated: Date? = null,
    val lastCompletedAt: Date? = null,
    val completionCount: Int = 0,
    val imageUrl: String? = null,
)

data class WishEntryFormState(
    val name: String = "",
    val purchased: Boolean = false,
    val url: String = "",
    val price: String = "",
    val currencyCode: String = "",
    val priority: WishListPriority = WishListPriority.PLANNED,
    val notes: String = "",
)

data class NoteEntryFormState(
    val name: String = "",
    val body: String = "",
)

sealed interface ListEntryDetailsUiEvent {
    data object EnterEditMode : ListEntryDetailsUiEvent
    data object RequestCancelEdit : ListEntryDetailsUiEvent
    data object ConfirmDiscard : ListEntryDetailsUiEvent
    data object DismissDiscardDialog : ListEntryDetailsUiEvent
    data class NameChanged(val value: String) : ListEntryDetailsUiEvent
    data object SaveClicked : ListEntryDetailsUiEvent

    sealed interface Routine : ListEntryDetailsUiEvent {
        data class RecurrenceUnitChanged(val value: String) : Routine
        data class IntervalChanged(val value: String) : Routine
        data class SelectedWeekdaysChanged(val dayNum: Int) : Routine
        data class ImageSelected(val uri: Uri) : Routine
    }

    sealed interface Wish : ListEntryDetailsUiEvent {
        data class PurchasedChanged(val value: Boolean) : Wish
        data class UrlChanged(val value: String) : Wish
        data class PriceChanged(val value: String) : Wish
        data class CurrencyCodeChanged(val value: String) : Wish
        data class PriorityChanged(val value: String) : Wish
        data class NotesChanged(val value: String) : Wish
    }

    sealed interface Note : ListEntryDetailsUiEvent {
        data class BodyChanged(val value: String) : Note
    }
}

sealed interface ListEntryDetailsNavigationEvent {
    data object NavigateBack : ListEntryDetailsNavigationEvent
}

sealed interface ListEntryDetailsCommand {
    data object NavigateBack : ListEntryDetailsCommand
}
