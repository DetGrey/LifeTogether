package com.example.lifetogether.ui.feature.lists.entryDetails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListEntryDetailsViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
) : ViewModel() {

    private var familyId: String? = null
    var listId: String by mutableStateOf("")

    // Form fields
    var entryId: String? by mutableStateOf(null)
    var name: String by mutableStateOf("")
    var recurrenceUnit: RecurrenceUnit by mutableStateOf(RecurrenceUnit.DAYS)
    var interval: Int by mutableIntStateOf(1)
    var weekdays: Set<Int> by mutableStateOf(emptySet())

    // UI state
    var isSaving: Boolean by mutableStateOf(false)
    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")

    fun setContext(familyId: String, listId: String) {
        this.familyId = familyId
        this.listId = listId
    }

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    fun save(onDone: () -> Unit) {
        val activeFamilyId = familyId
        if (activeFamilyId.isNullOrBlank()) {
            error = "Missing family context"
            showAlertDialog = true
            return
        }
        if (listId.isBlank()) {
            error = "Missing list context"
            showAlertDialog = true
            return
        }
        if (name.isBlank()) {
            error = "Name cannot be empty"
            showAlertDialog = true
            return
        }
        if (interval <= 0) {
            error = "Interval must be at least 1"
            showAlertDialog = true
            return
        }

        val now = Date()
        val draft = RoutineListEntry(
            id = entryId,
            familyId = activeFamilyId,
            listId = listId,
            itemName = name.trim(),
            lastUpdated = now,
            dateCreated = now,
            recurrenceUnit = recurrenceUnit,
            interval = interval,
            weekdays = weekdays.sorted(),
        ).let { entry ->
            entry.copy(nextDate = RecurrenceCalculator.nextDate(entry, now))
        }

        isSaving = true
        viewModelScope.launch {
            val result = if (entryId == null) {
                when (val r = saveItemUseCase(draft, Constants.ROUTINE_LIST_ENTRIES_TABLE)) {
                    is StringResultListener.Success -> ResultListener.Success
                    is StringResultListener.Failure -> ResultListener.Failure(r.message)
                }
            } else {
                updateItemUseCase(draft, Constants.ROUTINE_LIST_ENTRIES_TABLE)
            }

            isSaving = false
            when (result) {
                is ResultListener.Success -> onDone()
                is ResultListener.Failure -> {
                    error = result.message
                    showAlertDialog = true
                }
            }
        }
    }
}
