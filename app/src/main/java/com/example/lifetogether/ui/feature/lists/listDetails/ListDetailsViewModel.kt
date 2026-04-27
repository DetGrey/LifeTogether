package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.item.DeleteRoutineListEntriesUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ListDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val userListRepository: UserListRepository,
    private val deleteRoutineListEntriesUseCase: DeleteRoutineListEntriesUseCase,
) : ViewModel() {
    val listId: String = checkNotNull(savedStateHandle["listId"])

    private var familyId: String? = null
    private var uid: String? = null
    private var entriesJob: Job? = null
    private var listsJob: Job? = null
    private val imageJobs: MutableMap<String, Job> = mutableMapOf()

    private val _uiState = MutableStateFlow<ListDetailsUiState>(ListDetailsUiState.Loading)
    private val _entries = MutableStateFlow<List<RoutineListEntry>>(emptyList())
    private val _imageBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    val screenState: StateFlow<ListDetailsScreenState> = combine(
        _uiState,
        _entries,
        _imageBitmaps,
    ) { uiState, entries, imageBitmaps ->
        ListDetailsScreenState(
            uiState = uiState,
            entries = entries,
            imageBitmaps = imageBitmaps,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListDetailsScreenState(),
    )

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                val newFamilyId = authenticated?.user?.familyId
                val newUid = authenticated?.user?.uid
                if (newFamilyId != null && newUid != null &&
                    (newFamilyId != familyId || newUid != uid)
                ) {
                    familyId = newFamilyId
                    uid = newUid
                    observeListDetails()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                }
            }
        }
    }

    private fun observeListDetails() {
        val familyIdValue = familyId ?: return

        listsJob?.cancel()
        listsJob = viewModelScope.launch {
            userListRepository.observeUserLists(familyId = familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val foundList = result.data.firstOrNull { it.id == listId }

                        if (foundList != null) {
                            when (val currentState = _uiState.value) {
                                is ListDetailsUiState.Loading -> {
                                    _uiState.value = ListDetailsUiState.Content(listName = foundList.itemName)
                                }

                                is ListDetailsUiState.Content -> {
                                    _uiState.value = currentState.copy(listName = foundList.itemName)
                                }
                            }
                        }
                    }
                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }

        entriesJob?.cancel()
        entriesJob = viewModelScope.launch {
            userListRepository.observeRoutineListEntries(familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val sortedEntries = result.data
                            .sortedWith(compareBy(nullsLast()) { it.nextDate })

                        _entries.value = sortedEntries
                        updateImageJobs(sortedEntries)

                        if (_uiState.value is ListDetailsUiState.Loading) {
                            _uiState.value = ListDetailsUiState.Content()
                        }

                        syncSelectionState(sortedEntries)
                    }

                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    fun toggleActionSheet(show: Boolean? = null) {
        updateContentState { state ->
            state.copy(showActionSheet = show ?: !state.showActionSheet)
        }
    }

    fun startSelectionMode() {
        updateContentState { state ->
            applySelectionState(
                state = state.copy(showActionSheet = false),
                selectedEntryIds = state.selectedEntryIds,
                isSelectionModeActive = true,
            )
        }
    }

    fun enterSelectionMode(initialEntryId: String) {
        updateContentState { state ->
            applySelectionState(
                state = state.copy(showActionSheet = false),
                selectedEntryIds = state.selectedEntryIds + initialEntryId,
                isSelectionModeActive = true,
            )
        }
    }

    fun exitSelectionMode() {
        updateContentState { state ->
            state.copy(
                isSelectionModeActive = false,
                selectedEntryIds = emptySet(),
                isAllEntriesSelected = false,
                showActionSheet = false,
                showDeleteSelectedDialog = false,
            )
        }
    }

    fun toggleEntrySelection(entryId: String) {
        updateContentState { state ->
            val updatedSelection = if (state.selectedEntryIds.contains(entryId)) {
                state.selectedEntryIds - entryId
            } else {
                state.selectedEntryIds + entryId
            }

            applySelectionState(
                state,
                selectedEntryIds = updatedSelection,
                isSelectionModeActive = state.isSelectionModeActive,
            )
        }
    }

    fun toggleAllEntrySelection() {
        updateContentState { state ->
            val allEntryIds = _entries.value.mapNotNull { it.id }.toSet()
            if (allEntryIds.isEmpty()) return@updateContentState state

            if (state.isAllEntriesSelected) {
                state.copy(
                    isSelectionModeActive = false,
                    selectedEntryIds = emptySet(),
                    isAllEntriesSelected = false,
                    showDeleteSelectedDialog = false,
                )
            } else {
                applySelectionState(
                    state,
                    selectedEntryIds = allEntryIds,
                    isSelectionModeActive = true,
                )
            }
        }
    }

    fun requestDeleteSelected() {
        updateContentState { state ->
            if (state.selectedEntryIds.isEmpty()) {
                state.copy(showActionSheet = false)
            } else {
                state.copy(
                    showActionSheet = false,
                    showDeleteSelectedDialog = true,
                )
            }
        }
    }

    fun dismissDeleteSelectedDialog() {
        updateContentState { state ->
            state.copy(showDeleteSelectedDialog = false)
        }
    }

    fun confirmDeleteSelected() {
        val selectedEntries = _entries.value.filter { it.id in selectedEntryIds() }
        if (selectedEntries.isEmpty()) {
            dismissDeleteSelectedDialog()
            return
        }

        viewModelScope.launch {
            when (val result = deleteRoutineListEntriesUseCase(selectedEntries)) {
                is Result.Success -> {
                    updateContentState { state ->
                        state.copy(
                            isSelectionModeActive = false,
                            selectedEntryIds = emptySet(),
                            isAllEntriesSelected = false,
                            showActionSheet = false,
                            showDeleteSelectedDialog = false,
                        )
                    }
                }

                is Result.Failure -> {
                    updateContentState { state ->
                        state.copy(
                            showActionSheet = false,
                            showDeleteSelectedDialog = false,
                        )
                    }
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    fun completeEntry(entry: RoutineListEntry) {
        val updatedEntry = RecurrenceCalculator.applyCompletion(entry, completedAt = Date())
        viewModelScope.launch {
            when (val result = userListRepository.updateRoutineListEntry(updatedEntry)) {
                is Result.Success -> Unit
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun updateImageJobs(entries: List<RoutineListEntry>) {
        val newIds = entries.mapNotNull { it.id }.toSet()
        val currentIds = imageJobs.keys.toSet()

        (currentIds - newIds).forEach { entryId ->
            imageJobs.remove(entryId)?.cancel()
            _imageBitmaps.update { currentBitmaps ->
                currentBitmaps - entryId
            }
        }

        (newIds - currentIds).forEach { entryId ->
            imageJobs[entryId] = viewModelScope.launch {
                userListRepository.observeRoutineImageByteArray(entryId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _imageBitmaps.update { currentBitmaps ->
                                currentBitmaps + (entryId to result.data.toBitmap())
                            }
                        }

                        is Result.Failure -> {
                            _imageBitmaps.update { currentBitmaps ->
                                currentBitmaps - entryId
                            }
                        }
                    }
                }
            }
        }
    }

    private fun selectedEntryIds(): Set<String> {
        return (_uiState.value as? ListDetailsUiState.Content)?.selectedEntryIds.orEmpty()
    }

    private fun syncSelectionState(entries: List<RoutineListEntry>) {
        updateContentState { state ->
            applySelectionState(
                state = state,
                selectedEntryIds = state.selectedEntryIds,
                isSelectionModeActive = state.isSelectionModeActive,
                validEntryIds = entries.mapNotNull { it.id }.toSet(),
            )
        }
    }

    private fun updateContentState(
        update: (ListDetailsUiState.Content) -> ListDetailsUiState.Content,
    ) {
        _uiState.update { state ->
            when (state) {
                is ListDetailsUiState.Loading -> state
                is ListDetailsUiState.Content -> update(state)
            }
        }
    }

    private fun applySelectionState(
        state: ListDetailsUiState.Content,
        selectedEntryIds: Set<String>,
        isSelectionModeActive: Boolean,
        validEntryIds: Set<String> = _entries.value.mapNotNull { it.id }.toSet(),
    ): ListDetailsUiState.Content {
        val validSelection = selectedEntryIds.intersect(validEntryIds)

        return state.copy(
            isSelectionModeActive = isSelectionModeActive && validEntryIds.isNotEmpty(),
            selectedEntryIds = validSelection,
            isAllEntriesSelected = validEntryIds.isNotEmpty() && validSelection.size == validEntryIds.size,
            showDeleteSelectedDialog = state.showDeleteSelectedDialog && validSelection.isNotEmpty(),
        )
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
}
