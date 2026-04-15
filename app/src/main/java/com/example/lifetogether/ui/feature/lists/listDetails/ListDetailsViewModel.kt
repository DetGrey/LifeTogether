package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.usecase.image.FetchImageByteArrayUseCase
import com.example.lifetogether.domain.usecase.item.DeleteRoutineListEntriesUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class ListDetailsViewModel @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val fetchImageByteArrayUseCase: FetchImageByteArrayUseCase,
    private val deleteRoutineListEntriesUseCase: DeleteRoutineListEntriesUseCase,
) : ViewModel() {

    data class ListDetailsScreenState(
        val uiState: ListDetailsUiState = ListDetailsUiState.Loading,
        val entries: List<RoutineListEntry> = emptyList(),
        val imageBitmaps: Map<String, Bitmap> = emptyMap(),
    )

    private var familyId: String? = null
    private var uid: String? = null
    private var currentListId: String? = null
    private var entriesJob: Job? = null
    private var listsJob: Job? = null
    private val imageJobs: MutableMap<String, Job> = mutableMapOf()

    private val _uiState = MutableStateFlow<ListDetailsUiState>(ListDetailsUiState.Loading)
    private val _entries = MutableStateFlow<List<RoutineListEntry>>(emptyList())
    private val _imageBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())

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

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            updateContentState {
                it.copy(
                    showAlertDialog = false,
                    error = "",
                )
            }
        }
    }

    fun setUp(addedFamilyId: String, addedUid: String, listId: String) {
        if (familyId == addedFamilyId && uid == addedUid && currentListId == listId && entriesJob != null) return

        familyId = addedFamilyId
        uid = addedUid
        currentListId = listId
        _uiState.value = ListDetailsUiState.Loading

        listsJob?.cancel()
        listsJob = viewModelScope.launch {
            fetchListItemsUseCase(
                familyId = addedFamilyId,
                listName = Constants.USER_LISTS_TABLE,
                itemType = UserList::class,
                uid = addedUid,
            ).collect { result ->
                if (result is ListItemsResultListener.Success) {
                    val foundList = result.listItems
                        .filterIsInstance<UserList>()
                        .firstOrNull { it.id == listId }

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
            }
        }

        entriesJob?.cancel()
        entriesJob = viewModelScope.launch {
            fetchListItemsUseCase(
                familyId = addedFamilyId,
                listName = Constants.ROUTINE_LIST_ENTRIES_TABLE,
                itemType = RoutineListEntry::class,
                uid = listId,
            ).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success -> {
                        val sortedEntries = result.listItems
                            .filterIsInstance<RoutineListEntry>()
                            .sortedWith(compareBy(nullsLast()) { it.nextDate })

                        _entries.value = sortedEntries
                        updateImageJobs(sortedEntries, addedFamilyId)

                        if (_uiState.value is ListDetailsUiState.Loading) {
                            _uiState.value = ListDetailsUiState.Content()
                        }

                        syncSelectionState(sortedEntries)
                    }

                    is ListItemsResultListener.Failure -> showError(result.message)
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
                is ResultListener.Success -> {
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

                is ResultListener.Failure -> {
                    updateContentState { state ->
                        state.copy(
                            showActionSheet = false,
                            showDeleteSelectedDialog = false,
                        )
                    }
                    showError(result.message)
                }
            }
        }
    }

    fun completeEntry(entry: RoutineListEntry) {
        val updatedEntry = RecurrenceCalculator.applyCompletion(entry, completedAt = Date())
        viewModelScope.launch {
            when (val result = updateItemUseCase(updatedEntry, Constants.ROUTINE_LIST_ENTRIES_TABLE)) {
                is ResultListener.Success -> Unit
                is ResultListener.Failure -> showError(result.message)
            }
        }
    }

    private fun updateImageJobs(entries: List<RoutineListEntry>, familyId: String) {
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
                fetchImageByteArrayUseCase(
                    ImageType.RoutineListEntryImage(familyId, entryId),
                ).collect { result ->
                    when (result) {
                        is ByteArrayResultListener.Success -> {
                            _imageBitmaps.update { currentBitmaps ->
                                currentBitmaps + (entryId to result.byteArray.toBitmap())
                            }
                        }

                        is ByteArrayResultListener.Failure -> {
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
        _uiState.update { state ->
            when (state) {
                is ListDetailsUiState.Loading -> {
                    ListDetailsUiState.Content(
                        showAlertDialog = true,
                        error = message,
                    )
                }

                is ListDetailsUiState.Content -> {
                    state.copy(
                        showAlertDialog = true,
                        error = message,
                    )
                }
            }
        }
    }
}
