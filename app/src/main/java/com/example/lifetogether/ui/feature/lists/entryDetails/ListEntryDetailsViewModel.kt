package com.example.lifetogether.ui.feature.lists.entryDetails

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ItemResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListEntryDetailsViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
) : ViewModel() {

    companion object {
        val RECURRENCE_UNIT_STRINGS: List<String> = RecurrenceUnit.entries.map { it.name.lowercase() }
        val WEEKDAYS: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    data class EntryFormState(
        val name: String = "",
        val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAYS,
        val interval: String = "1",
        val selectedWeekdays: Set<Int> = emptySet(),
    )

    data class EntryDetailsScreenState(
        val uiState: EntryDetailsUiState = EntryDetailsUiState.Loading,
        val formState: EntryFormState = EntryFormState(),
    )

    private var familyId: String? = null
    private var listId: String? = null
    private var entryId: String? = null
    private var originalFormState: EntryFormState? = null

    private val _uiState = MutableStateFlow<EntryDetailsUiState>(EntryDetailsUiState.Loading)
    private val _formState = MutableStateFlow(EntryFormState())

    val screenState: StateFlow<EntryDetailsScreenState> = combine(_uiState, _formState) { ui, form ->
        EntryDetailsScreenState(ui, form)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EntryDetailsScreenState(),
    )

    fun setUp(familyId: String, listId: String, entryId: String?, onLoadFailed: () -> Unit) {
        Log.d("ListEntryDetailsVM", "familyId: $familyId, listId: $listId, entryId: $entryId")
        if (this.familyId == familyId && this.listId == listId && this.entryId == entryId) return
        this.familyId = familyId
        this.listId = listId
        this.entryId = entryId

        if (entryId == null) {
            originalFormState = EntryFormState()
            _uiState.value = EntryDetailsUiState.Content(isEditing = true)
        } else {
            _uiState.value = EntryDetailsUiState.Loading
            loadEntry(familyId, entryId, onLoadFailed)
        }
    }

    private fun loadEntry(familyId: String, entryId: String, onLoadFailed: () -> Unit) {
        Log.d("ListEntryDetailsVM", "Loading Entry. familyId: $familyId, listId: $listId, entryId: $entryId")
        viewModelScope.launch {
            fetchItemByIdUseCase(
                familyId = familyId,
                id = entryId,
                listName = Constants.ROUTINE_LIST_ENTRIES_TABLE,
                itemType = RoutineListEntry::class,
                uid = listId,
            ).collect { result ->
                Log.d("ListEntryDetailsScreen", "Result: $result")
                when (result) {
                    is ItemResultListener.Success -> {
                        val entry = result.item as? RoutineListEntry
                        if (entry != null) {
                            val loaded = EntryFormState(
                                name = entry.itemName,
                                recurrenceUnit = entry.recurrenceUnit,
                                interval = entry.interval.toString(),
                                selectedWeekdays = entry.weekdays.toSet(),
                            )
                            originalFormState = loaded
                            _formState.value = loaded
                            _uiState.value = EntryDetailsUiState.Content()
                        } else {
                            onLoadFailed()
                        }
                    }
                    is ItemResultListener.Failure -> onLoadFailed()
                }
            }
        }
    }

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { state ->
                if (state is EntryDetailsUiState.Content) state.copy(showAlertDialog = false, error = "") else state
            }
        }
    }

    fun enterEditMode() {
        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isEditing = true) else it }
    }

    fun requestCancelEdit() {
        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(showDiscardDialog = true) else it }
    }

    fun confirmDiscard() {
        _formState.value = originalFormState ?: EntryFormState()
        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isEditing = false, showDiscardDialog = false) else it }
    }

    fun dismissDiscardDialog() {
        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(showDiscardDialog = false) else it }
    }

    private fun showError(message: String) {
        _uiState.update { state ->
            if (state is EntryDetailsUiState.Content) state.copy(showAlertDialog = true, error = message) else state
        }
    }

    private fun validate(): String? {
        val form = _formState.value
        if (form.name.isBlank()) return "Name cannot be empty"
        val intervalInt = form.interval.toIntOrNull()
        if (intervalInt == null || intervalInt < 1) return "Interval must be at least 1"
        return null
    }

    fun save(onDone: () -> Unit) {
        val activeFamilyId = familyId ?: return showError("Missing family context")
        val activeListId = listId ?: return showError("Missing list context")

        validate()?.let { return showError(it) }

        val form = _formState.value
        val now = Date()
        val draft = RoutineListEntry(
            id = entryId,
            familyId = activeFamilyId,
            listId = activeListId,
            itemName = form.name.trim(),
            lastUpdated = now,
            dateCreated = now,
            recurrenceUnit = form.recurrenceUnit,
            interval = form.interval.toInt(),
            weekdays = form.selectedWeekdays.sorted(),
        ).let { entry ->
            entry.copy(nextDate = RecurrenceCalculator.nextDate(entry, now))
        }

        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isSaving = true) else it }
        viewModelScope.launch {
            val result = if (entryId == null) {
                when (val r = saveItemUseCase(draft, Constants.ROUTINE_LIST_ENTRIES_TABLE)) {
                    is StringResultListener.Success -> ResultListener.Success
                    is StringResultListener.Failure -> ResultListener.Failure(r.message)
                }
            } else {
                updateItemUseCase(draft, Constants.ROUTINE_LIST_ENTRIES_TABLE)
            }

            _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isSaving = false) else it }
            when (result) {
                is ResultListener.Success -> {
                    if (entryId == null) {
                        onDone()
                    } else {
                        originalFormState = _formState.value
                        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isEditing = false) else it }
                    }
                }
                is ResultListener.Failure -> showError(result.message)
            }
        }
    }

    fun onNameChange(value: String) {
        _formState.update { it.copy(name = value) }
    }

    fun onRecurrenceUnitChange(newUnit: String) {
        if (newUnit !in RECURRENCE_UNIT_STRINGS) return
        _formState.update { it.copy(recurrenceUnit = RecurrenceUnit.fromValue(newUnit)) }
    }

    fun onIntervalChange(value: String) {
        _formState.update { it.copy(interval = value.filter { c -> c.isDigit() }) }
    }

    fun onSelectedWeekdaysChange(dayNum: Int) {
        _formState.update { state ->
            val updated = if (dayNum in state.selectedWeekdays) {
                state.selectedWeekdays - dayNum
            } else {
                state.selectedWeekdays + dayNum
            }
            state.copy(selectedWeekdays = updated)
        }
    }
}
