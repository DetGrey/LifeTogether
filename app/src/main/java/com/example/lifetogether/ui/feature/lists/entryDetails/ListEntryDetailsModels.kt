package com.example.lifetogether.ui.feature.lists.entryDetails

import android.graphics.Bitmap
import android.net.Uri
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.lists.WishListPriority

sealed interface EntryDetailsUiState {
    data object Loading : EntryDetailsUiState

    data class Content(
        val details: EntryDetailsContent,
        val isEditing: Boolean,
        val showDiscardDialog: Boolean,
        val isSaving: Boolean,
        val showImageUploadDialog: Boolean,
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
                        isPurchased = entry.isPurchased,
                        url = entry.url.orEmpty(),
                        estimatedPriceMinor = entry.estimatedPriceMinor?.toString().orEmpty(),
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
                        markdownBody = entry.markdownBody,
                    ),
                )
            }
        }
    }

    data class Checklist(
        val form: ChecklistEntryFormState,
    ) : EntryDetailsContent {
        override val listType: ListType = ListType.CHECKLIST

        companion object {
            fun blank(): Checklist {
                return Checklist(form = ChecklistEntryFormState())
            }

            fun from(entry: ChecklistEntry): Checklist {
                return Checklist(
                    form = ChecklistEntryFormState(
                        name = entry.itemName,
                        isChecked = entry.isChecked,
                    ),
                )
            }
        }
    }

    data class Meal(
        val form: MealPlanEntryFormState,
    ) : EntryDetailsContent {
        override val listType: ListType = ListType.MEAL_PLANNER

        companion object {
            fun blank(): Meal {
                return Meal(form = MealPlanEntryFormState())
            }

            fun from(entry: MealPlanEntry): Meal {
                return Meal(
                    form = MealPlanEntryFormState(
                        name = entry.itemName,
                        date = entry.date,
                        recipeId = entry.recipeId.orEmpty(),
                        customMealName = entry.customMealName.orEmpty(),
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
)

data class WishEntryFormState(
    val name: String = "",
    val isPurchased: Boolean = false,
    val url: String = "",
    val estimatedPriceMinor: String = "",
    val currencyCode: String = "",
    val priority: WishListPriority = WishListPriority.PLANNED,
    val notes: String = "",
)

data class NoteEntryFormState(
    val name: String = "",
    val markdownBody: String = "",
    val isPreviewMode: Boolean = false,
)

data class ChecklistEntryFormState(
    val name: String = "",
    val isChecked: Boolean = false,
)

data class MealPlanEntryFormState(
    val name: String = "",
    val date: String = "",
    val recipeId: String = "",
    val customMealName: String = "",
)

sealed interface ListEntryDetailsUiEvent {
    data object EnterEditMode : ListEntryDetailsUiEvent
    data object RequestCancelEdit : ListEntryDetailsUiEvent
    data object ConfirmDiscard : ListEntryDetailsUiEvent
    data object DismissDiscardDialog : ListEntryDetailsUiEvent
    data class NameChanged(val value: String) : ListEntryDetailsUiEvent
    data object SaveClicked : ListEntryDetailsUiEvent
    data object RequestImageUpload : ListEntryDetailsUiEvent
    data object DismissImageUpload : ListEntryDetailsUiEvent
    data object ConfirmImageUpload : ListEntryDetailsUiEvent

    sealed interface Routine : ListEntryDetailsUiEvent {
        data class RecurrenceUnitChanged(val value: String) : Routine
        data class IntervalChanged(val value: String) : Routine
        data class SelectedWeekdaysChanged(val dayNum: Int) : Routine
        data class ImageSelected(val uri: Uri) : Routine
    }

    sealed interface Wish : ListEntryDetailsUiEvent {
        data class PurchasedChanged(val value: Boolean) : Wish
        data class UrlChanged(val value: String) : Wish
        data class EstimatedPriceMinorChanged(val value: String) : Wish
        data class CurrencyCodeChanged(val value: String) : Wish
        data class PriorityChanged(val value: String) : Wish
        data class NotesChanged(val value: String) : Wish
    }

    sealed interface Note : ListEntryDetailsUiEvent {
        data class MarkdownBodyChanged(val value: String) : Note
        data class PreviewModeChanged(val value: Boolean) : Note
    }

    sealed interface Checklist : ListEntryDetailsUiEvent {
        data class CheckedChanged(val value: Boolean) : Checklist
    }

    sealed interface Meal : ListEntryDetailsUiEvent {
        data class DateChanged(val value: String) : Meal
        data class RecipeIdChanged(val value: String) : Meal
        data class CustomMealNameChanged(val value: String) : Meal
    }
}

sealed interface ListEntryDetailsNavigationEvent {
    data object NavigateBack : ListEntryDetailsNavigationEvent
}
