package com.example.lifetogether.ui.feature.lists.entryDetails

import android.graphics.Bitmap
import android.net.Uri
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.lists.WishListPriority

sealed interface EntryDetailsUiState {
    data object Loading : EntryDetailsUiState

    data class Content(
        val details: EntryDetailsContent,
        val mealRecipeSearchState: MealRecipeSearchState = MealRecipeSearchState(),
        val isEditing: Boolean = false,
        val showDiscardDialog: Boolean = false,
        val isSaving: Boolean = false,
        val showImageUploadDialog: Boolean = false,
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
                        mealType = entry.mealType,
                        notes = entry.notes,
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
    val price: String = "",
    val currencyCode: String = "",
    val priority: WishListPriority = WishListPriority.PLANNED,
    val notes: String = "",
)

data class NoteEntryFormState(
    val name: String = "",
    val body: String = "",
)

data class RecipeSearchItem(
    val id: String,
    val itemName: String,
    val preparationTimeMin: Int,
)

enum class MealSearchMode {
    RECIPE,
    CUSTOM,
}

data class MealRecipeSearchState(
    val mode: MealSearchMode = MealSearchMode.RECIPE,
    val query: String = "",
    val isSearchFocused: Boolean = false,
    val selectedRecipeSearchItem: RecipeSearchItem? = null,
    val suggestions: List<RecipeSearchItem> = emptyList(),
)

data class MealPlanEntryFormState(
    val name: String = "",
    val date: String = "",
    val recipeId: String = "",
    val customMealName: String = "",
    val mealType: MealType = MealType.DINNER,
    val notes: String = "",
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
        data class PriceChanged(val value: String) : Wish
        data class CurrencyCodeChanged(val value: String) : Wish
        data class PriorityChanged(val value: String) : Wish
        data class NotesChanged(val value: String) : Wish
    }

    sealed interface Note : ListEntryDetailsUiEvent {
        data class BodyChanged(val value: String) : Note
    }

    sealed interface Meal : ListEntryDetailsUiEvent {
        data class DateChanged(val value: String) : Meal
        data class RecipeQueryChanged(val value: String) : Meal
        data class RecipeSearchFocusedChanged(val value: Boolean) : Meal
        data class RecipeSelected(val recipe: RecipeSearchItem) : Meal
        data class RecipeModeChanged(val mode: MealSearchMode) : Meal
        data class CustomMealNameChanged(val value: String) : Meal
        data class MealTypeChanged(val value: String) : Meal
        data class NotesChanged(val value: String) : Meal
    }
}

sealed interface ListEntryDetailsNavigationEvent {
    data object NavigateBack : ListEntryDetailsNavigationEvent
}
