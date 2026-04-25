package com.example.lifetogether.ui.feature.lists.entryDetails

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
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
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListEntryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val userListRepository: UserListRepository,
    private val uploadImageUseCase: UploadImageUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    companion object {
        val RECURRENCE_UNIT_STRINGS: List<String> = RecurrenceUnit.entries.map { it.name.lowercase() }
        val WEEKDAYS: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    private val listId: String = checkNotNull(savedStateHandle["listId"])
    val entryId: String? = savedStateHandle["entryId"]

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId: StateFlow<String?> = _familyId.asStateFlow()
    private var originalFormState: EntryFormState? = null

    private val _uiState = MutableStateFlow<EntryDetailsUiState>(EntryDetailsUiState.Loading)
    private val _formState = MutableStateFlow(EntryFormState())

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    val screenState: StateFlow<EntryDetailsScreenState> = combine(_uiState, _formState) { ui, form ->
        EntryDetailsScreenState(ui, form)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EntryDetailsScreenState(),
    )

    init {
        if (entryId == null) {
            originalFormState = EntryFormState()
            _uiState.value = EntryDetailsUiState.Content(isEditing = true)
        }

        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                val previousFamilyId = _familyId.value
                _familyId.value = newFamilyId

                if (newFamilyId != null && entryId != null && newFamilyId != previousFamilyId) {
                    loadEntry(entryId)
                }
            }
        }
    }

    private fun loadEntry(entryIdToLoad: String) {
        val familyIdValue = _familyId.value ?: return
        Log.d("ListEntryDetailsVM", "Loading Entry. familyId: $familyIdValue, listId: $listId, entryId: $entryIdToLoad")

        viewModelScope.launch {
            userListRepository.observeRoutineListEntry(entryIdToLoad).collect { result ->
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
                    is Result.Failure -> {
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    fun onUiEvent(event: ListEntryDetailsUiEvent) {
        when (event) {
            ListEntryDetailsUiEvent.EnterEditMode ->
                _uiState.updateContent { it.copy(isEditing = true) }

            ListEntryDetailsUiEvent.RequestCancelEdit ->
                _uiState.updateContent { it.copy(showDiscardDialog = true) }

            ListEntryDetailsUiEvent.DismissDiscardDialog ->
                _uiState.updateContent { it.copy(showDiscardDialog = false) }

            ListEntryDetailsUiEvent.RequestImageUpload ->
                _uiState.updateContent { it.copy(showImageUploadDialog = true) }

            ListEntryDetailsUiEvent.DismissImageUpload,
            ListEntryDetailsUiEvent.ConfirmImageUpload ->
                _uiState.updateContent { it.copy(showImageUploadDialog = false) }

            ListEntryDetailsUiEvent.ConfirmDiscard -> confirmDiscard()

            is ListEntryDetailsUiEvent.NameChanged ->
                _formState.update { it.copy(name = event.value) }

            is ListEntryDetailsUiEvent.RecurrenceUnitChanged -> onRecurrenceUnitChange(event.value)

            is ListEntryDetailsUiEvent.IntervalChanged ->
                _formState.update { it.copy(interval = event.value.filter { c -> c.isDigit() }) }

            is ListEntryDetailsUiEvent.SelectedWeekdaysChanged -> onSelectedWeekdaysChange(event.dayNum)

            ListEntryDetailsUiEvent.SaveClicked -> saveEntry()

            is ListEntryDetailsUiEvent.ImageSelected -> {
                onImageSelected(event.uri, context.contentResolver)
            }
        }
    }

    fun confirmDiscard() {
        _formState.value = originalFormState ?: EntryFormState()
        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isEditing = false, showDiscardDialog = false) else it }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }

    private fun validate(): String? {
        val form = _formState.value
        if (form.name.isBlank()) return "Name cannot be empty"
        val intervalInt = form.interval.toIntOrNull()
        if (intervalInt == null || intervalInt < 1) return "Interval must be at least 1"
        return null
    }

    fun saveEntry() {
        val activeFamilyId = _familyId.value ?: return showError("Missing family context")
        val activeListId = listId

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
                        }
                        _uiState.update { if (it is EntryDetailsUiState.Content) it.copy(isSaving = false) else it }
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

    fun onRecurrenceUnitChange(newUnit: String) {
        if (newUnit !in RECURRENCE_UNIT_STRINGS) return
        _formState.update { it.copy(recurrenceUnit = RecurrenceUnit.fromValue(newUnit)) }
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

    private fun MutableStateFlow<EntryDetailsUiState>.updateContent(
        block: (EntryDetailsUiState.Content) -> EntryDetailsUiState
    ) {
        this.update { state ->
            if (state is EntryDetailsUiState.Content) block(state) else state
        }
    }
}
