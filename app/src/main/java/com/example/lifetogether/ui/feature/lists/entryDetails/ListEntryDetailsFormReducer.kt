package com.example.lifetogether.ui.feature.lists.entryDetails

import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.WishListPriority
import javax.inject.Inject

class ListEntryDetailsFormReducer @Inject constructor() {
    fun reduce(
        details: EntryDetailsContent,
        event: ListEntryDetailsUiEvent,
    ): EntryDetailsContent {
        return when (details) {
            is EntryDetailsContent.Routine -> reduceRoutine(details, event)
            is EntryDetailsContent.Wish -> reduceWish(details, event)
            is EntryDetailsContent.Note -> reduceNote(details, event)
            is EntryDetailsContent.Meal -> reduceMeal(details, event)
        }
    }

    private fun reduceRoutine(
        details: EntryDetailsContent.Routine,
        event: ListEntryDetailsUiEvent,
    ): EntryDetailsContent {
        return when (event) {
            is ListEntryDetailsUiEvent.NameChanged -> details.copy(form = details.form.copy(name = event.value))
            is ListEntryDetailsUiEvent.Routine.RecurrenceUnitChanged -> details.copy(
                form = RecurrenceUnit.fromValue(event.value)?.let { unit ->
                    details.form.copy(recurrenceUnit = unit)
                } ?: details.form,
            )
            is ListEntryDetailsUiEvent.Routine.IntervalChanged -> details.copy(
                form = details.form.copy(interval = event.value.filter(Char::isDigit)),
            )
            is ListEntryDetailsUiEvent.Routine.SelectedWeekdaysChanged -> details.copy(
                form = details.form.copy(
                    selectedWeekdays = if (event.dayNum in details.form.selectedWeekdays) {
                        details.form.selectedWeekdays - event.dayNum
                    } else {
                        details.form.selectedWeekdays + event.dayNum
                    },
                ),
            )
            else -> details
        }
    }

    private fun reduceWish(
        details: EntryDetailsContent.Wish,
        event: ListEntryDetailsUiEvent,
    ): EntryDetailsContent {
        return when (event) {
            is ListEntryDetailsUiEvent.NameChanged -> details.copy(form = details.form.copy(name = event.value))
            is ListEntryDetailsUiEvent.Wish.PurchasedChanged -> details.copy(form = details.form.copy(isPurchased = event.value))
            is ListEntryDetailsUiEvent.Wish.UrlChanged -> details.copy(form = details.form.copy(url = event.value))
            is ListEntryDetailsUiEvent.Wish.PriceChanged -> details.copy(
                form = details.form.copy(price = event.value.filter { it.isDigit() || it == '.' }),
            )
            is ListEntryDetailsUiEvent.Wish.CurrencyCodeChanged -> details.copy(form = details.form.copy(currencyCode = event.value))
            is ListEntryDetailsUiEvent.Wish.PriorityChanged -> details.copy(
                form = WishListPriority.fromValue(event.value)?.let { priority ->
                    details.form.copy(priority = priority)
                } ?: details.form,
            )
            is ListEntryDetailsUiEvent.Wish.NotesChanged -> details.copy(form = details.form.copy(notes = event.value))
            else -> details
        }
    }

    private fun reduceNote(
        details: EntryDetailsContent.Note,
        event: ListEntryDetailsUiEvent,
    ): EntryDetailsContent {
        return when (event) {
            is ListEntryDetailsUiEvent.NameChanged -> details.copy(form = details.form.copy(name = event.value))
            is ListEntryDetailsUiEvent.Note.BodyChanged -> details.copy(form = details.form.copy(body = event.value))
            else -> details
        }
    }

    private fun reduceMeal(
        details: EntryDetailsContent.Meal,
        event: ListEntryDetailsUiEvent,
    ): EntryDetailsContent {
        return when (event) {
            is ListEntryDetailsUiEvent.NameChanged -> details.copy(form = details.form.copy(name = event.value))
            is ListEntryDetailsUiEvent.Meal.DateChanged -> details.copy(form = details.form.copy(date = event.value))
            is ListEntryDetailsUiEvent.Meal.CustomMealNameChanged -> details.copy(
                form = details.form.copy(
                    name = event.value,
                    customMealName = event.value,
                ),
            )
            is ListEntryDetailsUiEvent.Meal.MealTypeChanged -> details.copy(
                form = MealType.fromDisplayName(event.value)?.let { mealType ->
                    details.form.copy(mealType = mealType)
                } ?: details.form,
            )
            is ListEntryDetailsUiEvent.Meal.NotesChanged -> details.copy(form = details.form.copy(notes = event.value))
            else -> details
        }
    }
}
