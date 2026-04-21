package com.example.lifetogether.ui.feature.lists.entryDetails

import com.example.lifetogether.domain.result.toUserMessage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListEntryDetailsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userListRepository: UserListRepository,
    private val uploadImageUseCase: UploadImageUseCase,
    @ApplicationContext private val context: Context,
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
        val pendingImageUri: Uri? = null,
        val pendingImageBitmap: Bitmap? = null,
    )

    data class EntryDetailsScreenState(
        val uiState: EntryDetailsUiState = EntryDetailsUiState.Loading,
        val formState: EntryFormState = EntryFormState(),
    )

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId: StateFlow<String?> = _familyId.asStateFlow()
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

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                _familyId.value = (state as? SessionState.Authenticated)?.user?.familyId
            }
        }
    }

    fun setUp(listId: String, entryId: String?, onLoadFailed: () -> Unit) {
        val familyIdValue = _familyId.value
        Log.d("ListEntryDetailsVM", "familyId: $familyIdValue, listId: $listId, entryId: $entryId")
        if (this.listId == listId && this.entryId == entryId) return
        this.listId = listId
        this.entryId = entryId

        if (entryId == null) {
            originalFormState = EntryFormState()
            _uiState.value = EntryDetailsUiState.Content(isEditing = true)
        } else {
            _uiState.value = EntryDetailsUiState.Loading
            loadEntry(entryId, onLoadFailed)
        }
    }

    private fun loadEntry(entryId: String, onLoadFailed: () -> Unit) {
        val familyIdValue = _familyId.value ?: run { onLoadFailed(); return }
        Log.d("ListEntryDetailsVM", "Loading Entry. familyId: $familyIdValue, listId: $listId, entryId: $entryId")
        viewModelScope.launch {
            userListRepository.observeRoutineListEntry(entryId).collect { result ->
                Log.d("ListEntryDetailsScreen", "Result: $result")
                when (result) {
                    is Result.Success -> {
                        val entry = result.data
                        val loaded = EntryFormState(
                            name = entry.itemName,
                            recurrenceUnit = entry.recurrenceUnit,
                            interval = entry.interval.toString(),
                            selectedWeekdays = entry.weekdays.toSet(),
                        )
                        originalFormState = loaded
                        _formState.value = loaded
                        _uiState.value = EntryDetailsUiState.Content()
                    }
                    is Result.Failure -> onLoadFailed()
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
        val activeFamilyId = _familyId.value ?: return showError("Missing family context")
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
            if (entryId == null) {
                when (val r = userListRepository.saveRoutineListEntry(draft)) {
                    is Result.Success -> {
                        val pendingUri = form.pendingImageUri
                        if (pendingUri != null) {
                            uploadImageUseCase.invoke(
                                pendingUri,
                                ImageType.RoutineListEntryImage(activeFamilyId, r.data),
                                context,
                            )
                            // Result ignored — entry is already saved; image syncs via observer
                        }
                        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isSaving = false) else it }
                        onDone()
                    }
                    is Result.Failure -> {
                        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isSaving = false) else it }
                        showError(r.error.toUserMessage())
                    }
                }
            } else {
                val result = userListRepository.updateRoutineListEntry(draft)
                _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isSaving = false) else it }
                when (result) {
                    is Result.Success -> {
                        originalFormState = _formState.value
                        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isEditing = false) else it }
                    }
                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    fun onImageSelected(uri: Uri, contentResolver: ContentResolver) {
        val bitmap = uri.toBitmap(contentResolver)
        _formState.update { it.copy(pendingImageUri = uri, pendingImageBitmap = bitmap) }
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
