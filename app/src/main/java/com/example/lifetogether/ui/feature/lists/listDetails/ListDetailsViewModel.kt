package com.example.lifetogether.ui.feature.lists.listDetails

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.RecurrenceCalculator
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.ListEntry
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.item.DeleteRoutineListEntriesUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import java.util.Date
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
    private val userListRepository: UserListRepository,
    private val deleteRoutineListEntriesUseCase: DeleteRoutineListEntriesUseCase,
) : ViewModel() {
    val listId: String = checkNotNull(savedStateHandle["listId"])

    private val selectionState = MutableStateFlow(ListDetailsSelectionState())

    private val contentState: StateFlow<ListDetailsContentSnapshot?> = sessionRepository.sessionState
        .map { state ->
            state.authenticatedUserOrNull?.let { user ->
                val uid = user.uid ?: return@let null
                val familyId = user.familyId ?: return@let null
                SessionContext(
                    uid = uid,
                    familyId = familyId,
                )
            }
        }
        .distinctUntilChanged()
        .flatMapLatest { context ->
            if (context == null) {
                flowOf(null)
            } else {
                observeSelectedListContent(context)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    val uiState: StateFlow<ListDetailsUiState> = combine(
        contentState,
        selectionState,
    ) { content, selection ->
        content?.toUiState(selection) ?: ListDetailsUiState.Loading
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListDetailsUiState.Loading,
    )

    fun toggleActionSheet(show: Boolean? = null) {
        updateSelectionStateIfContent { state, _ ->
            state.copy(showActionSheet = show ?: !state.showActionSheet)
        }
    }

    fun startSelectionMode() {
        updateSelectionStateIfContent { state, _ ->
            state.copy(
                isSelectionModeActive = true,
                showActionSheet = false,
            )
        }
    }

    fun enterSelectionMode(initialEntryId: String) {
        updateSelectionStateIfContent { state, _ ->
            state.copy(
                isSelectionModeActive = true,
                selectedEntryIds = state.selectedEntryIds + initialEntryId,
                showActionSheet = false,
            )
        }
    }

    fun exitSelectionMode() {
        selectionState.value = ListDetailsSelectionState()
    }

    fun toggleEntrySelection(entryId: String) {
        updateSelectionStateIfContent { state, _ ->
            val updatedSelection = if (entryId in state.selectedEntryIds) {
                state.selectedEntryIds - entryId
            } else {
                state.selectedEntryIds + entryId
            }

            state.copy(
                isSelectionModeActive = true,
                selectedEntryIds = updatedSelection,
            )
        }
    }

    fun toggleAllEntrySelection() {
        updateSelectionStateIfContent { state, content ->
            val allEntryIds = content.entries.map { it.id }.toSet()
            if (allEntryIds.isEmpty()) {
                state
            } else if (state.selectedEntryIds.containsAll(allEntryIds)) {
                ListDetailsSelectionState()
            } else {
                state.copy(
                    isSelectionModeActive = true,
                    selectedEntryIds = allEntryIds,
                    showActionSheet = false,
                )
            }
        }
    }

    fun requestDeleteSelected() {
        updateSelectionStateIfContent { state, _ ->
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
        updateSelectionStateIfContent { state, _ ->
            state.copy(showDeleteSelectedDialog = false)
        }
    }

    fun confirmDeleteSelected() {
        val currentState = contentState.value ?: return
        val selectedEntryIds = selectionState.value.selectedEntryIds
        val selectedEntries = currentState.entries.filter { it.id in selectedEntryIds }
        if (selectedEntries.isEmpty()) {
            dismissDeleteSelectedDialog()
            return
        }

        viewModelScope.launch {
            val result = when (currentState.listType) {
                ListType.ROUTINE -> deleteRoutineListEntriesUseCase(selectedEntries.filterIsInstance<RoutineListEntry>())
                ListType.WISH_LIST -> userListRepository.deleteWishListEntries(selectedEntries.map { it.id })
                ListType.NOTES -> userListRepository.deleteNoteEntries(selectedEntries.map { it.id })
                ListType.CHECKLIST -> userListRepository.deleteChecklistEntries(selectedEntries.map { it.id })
                ListType.MEAL_PLANNER -> userListRepository.deleteMealPlanEntries(selectedEntries.map { it.id })
            }

            when (result) {
                is Result.Success -> {
                    exitSelectionMode()
                }

                is Result.Failure -> {
                    selectionState.update { state ->
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

    fun toggleEntryCompleted(entryId: String) {
        val entry = contentState.value?.entries?.firstOrNull { it.id == entryId } ?: return
        viewModelScope.launch {
            val result = when (entry) {
                is RoutineListEntry -> {
                    val updatedEntry = RecurrenceCalculator.applyCompletion(entry, completedAt = Date())
                    userListRepository.updateRoutineListEntry(updatedEntry)
                }

                is ChecklistEntry -> userListRepository.updateChecklistEntry(
                    entry.copy(
                        isChecked = !entry.isChecked,
                        lastUpdated = Date(),
                    ),
                )

                is WishListEntry -> userListRepository.updateWishListEntry(
                    entry.copy(
                        isPurchased = !entry.isPurchased,
                        lastUpdated = Date(),
                    ),
                )

                else -> Result.Success(Unit)
            }

            when (result) {
                is Result.Success -> Unit
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun observeSelectedListContent(context: SessionContext): Flow<ListDetailsContentSnapshot?> {
        return userListRepository.observeUserLists(context.familyId)
            .successData()
            .map { lists -> lists.firstOrNull { it.id == listId } }
            .flatMapLatest { list ->
                if (list == null) {
                    flowOf(null)
                } else {
                    observeListEntries(context.familyId, list).flatMapLatest { entries ->
                        if (list.type == ListType.ROUTINE) {
                            observeRoutineImageBitmaps(entries.filterIsInstance<RoutineListEntry>())
                                .map { imageBitmaps ->
                                    list.toContentSnapshot(entries, imageBitmaps)
                                }
                        } else {
                            flowOf(list.toContentSnapshot(entries, emptyMap()))
                        }
                    }
                }
            }
    }

    private fun observeListEntries(
        familyId: String,
        list: UserList,
    ): Flow<List<ListEntry>> {
        return when (list.type) {
            ListType.ROUTINE -> userListRepository.observeRoutineListEntriesByListId(familyId, listId)
            ListType.WISH_LIST -> userListRepository.observeWishListEntriesByListId(familyId, listId)
            ListType.NOTES -> userListRepository.observeNoteEntriesByListId(familyId, listId)
            ListType.CHECKLIST -> userListRepository.observeChecklistEntriesByListId(familyId, listId)
            ListType.MEAL_PLANNER -> userListRepository.observeMealPlanEntriesByListId(familyId, listId)
        }.successData()
    }

    private fun <T> Flow<Result<T, AppError>>.successData(): Flow<T> {
        return transformLatest { result ->
            when (result) {
                is Result.Success -> emit(result.data)
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun observeRoutineImageBitmaps(entries: List<RoutineListEntry>): Flow<Map<String, Bitmap>> {
        return if (entries.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(
                entries.map { entry ->
                    userListRepository.observeRoutineImageByteArray(entry.id)
                        .map { result ->
                            when (result) {
                                is Result.Success -> entry.id to result.data.toBitmap()
                                is Result.Failure -> entry.id to null
                            }
                        }
                },
            ) { bitmaps ->
                bitmaps.mapNotNull { (entryId, bitmap) ->
                    bitmap?.let { entryId to it }
                }.toMap()
            }
        }
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

    private inline fun updateSelectionStateIfContent(
        crossinline block: (ListDetailsSelectionState, ListDetailsContentSnapshot) -> ListDetailsSelectionState,
    ) {
        val content = contentState.value ?: return
        selectionState.update { state ->
            block(state, content)
        }
    }

    private fun UserList.toContentSnapshot(
        entries: List<ListEntry>,
        imageBitmaps: Map<String, Bitmap>,
    ): ListDetailsContentSnapshot {
        return ListDetailsContentSnapshot(
            listName = itemName,
            listType = type,
            entries = entries,
            imageBitmaps = imageBitmaps,
        )
    }

    private fun ListDetailsContentSnapshot.toUiState(
        selectionState: ListDetailsSelectionState,
    ): ListDetailsUiState.Content {
        val validEntryIds = entries.map { it.id }.toSet()
        val selectedEntryIds = selectionState.selectedEntryIds.intersect(validEntryIds)

        return ListDetailsUiState.Content(
            listName = listName,
            listType = listType,
            entries = entries,
            imageBitmaps = imageBitmaps,
            isSelectionModeActive = selectionState.isSelectionModeActive && validEntryIds.isNotEmpty(),
            selectedEntryIds = selectedEntryIds,
            isAllEntriesSelected = validEntryIds.isNotEmpty() && selectedEntryIds.size == validEntryIds.size,
            showActionSheet = selectionState.showActionSheet,
            showDeleteSelectedDialog = selectionState.showDeleteSelectedDialog && selectedEntryIds.isNotEmpty(),
        )
    }

    private data class SessionContext(
        val uid: String,
        val familyId: String,
    )

    private data class ListDetailsContentSnapshot(
        val listName: String,
        val listType: ListType,
        val entries: List<ListEntry>,
        val imageBitmaps: Map<String, Bitmap>,
    )

    private data class ListDetailsSelectionState(
        val isSelectionModeActive: Boolean = false,
        val selectedEntryIds: Set<String> = emptySet(),
        val showActionSheet: Boolean = false,
        val showDeleteSelectedDialog: Boolean = false,
    )
}
