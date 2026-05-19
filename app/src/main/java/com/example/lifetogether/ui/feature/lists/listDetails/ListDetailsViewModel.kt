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
import com.example.lifetogether.domain.model.lists.NoteEntry
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
import com.example.lifetogether.ui.navigation.ListDetailNavRoute
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
import java.util.UUID
import javax.inject.Inject
import androidx.navigation.toRoute

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ListDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository,
    private val userListRepository: UserListRepository,
    private val deleteRoutineListEntriesUseCase: DeleteRoutineListEntriesUseCase,
) : ViewModel() {
    val listId: String = savedStateHandle.toRoute<ListDetailNavRoute>().listId

    private var currentList: UserList? = null
    private val selectionState = MutableStateFlow(ListDetailsSelectionState())
    private val checklistEditorState = MutableStateFlow(ChecklistEditorState())

    private val contentState: StateFlow<ListDetailsContentSnapshot?> = sessionRepository.sessionState
        .map { state ->
            state.authenticatedUserOrNull?.let { user ->
                val uid = user.uid
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
        checklistEditorState,
    ) { content, selection, checklistEditor ->
        val state = content?.toUiState(selection, checklistEditor) ?: ListDetailsUiState.Loading
        state
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ListDetailsUiState.Loading,
    )

    fun onUiEvent(event: ListDetailsUiEvent) {
        when (event) {
            ListDetailsUiEvent.ToggleActionSheet -> toggleActionSheet()
            ListDetailsUiEvent.StartSelectionMode -> startSelectionMode()
            is ListDetailsUiEvent.EnterSelectionMode -> enterSelectionMode(event.entryId)
            ListDetailsUiEvent.ExitSelectionMode -> exitSelectionMode()
            is ListDetailsUiEvent.ToggleEntrySelection -> toggleEntrySelection(event.entryId)
            ListDetailsUiEvent.ToggleAllEntrySelection -> toggleAllEntrySelection()
            ListDetailsUiEvent.RequestDeleteSelected -> requestDeleteSelected()
            ListDetailsUiEvent.DismissDeleteSelectedDialog -> dismissDeleteSelectedDialog()
            ListDetailsUiEvent.ConfirmDeleteSelected -> confirmDeleteSelected()
            ListDetailsUiEvent.RequestRenameList -> requestRenameList()
            ListDetailsUiEvent.DismissRenameListDialog -> dismissRenameListDialog()
            is ListDetailsUiEvent.RenameListNameChanged -> updateRenameListText(event.value)
            ListDetailsUiEvent.ConfirmRenameList -> confirmRenameList()
            is ListDetailsUiEvent.ToggleEntryCompleted -> toggleEntryCompleted(event.entryId)
            is ListDetailsUiEvent.Checklist.EditRequested -> startEditingChecklistItem(event.entryId)
            is ListDetailsUiEvent.Checklist.NameChanged -> updateChecklistDraftName(event.value)
            ListDetailsUiEvent.Checklist.ActionClicked -> saveChecklistDraft()
        }
    }

    private fun toggleActionSheet(show: Boolean? = null) {
        updateSelectionStateIfContent { state, _ ->
            state.copy(showActionSheet = show ?: !state.showActionSheet)
        }
    }

    private fun startSelectionMode() {
        clearChecklistEditorState()
        updateSelectionStateIfContent { state, _ ->
            state.copy(
                isSelectionModeActive = true,
                showActionSheet = false,
            )
        }
    }

    private fun enterSelectionMode(initialEntryId: String) {
        clearChecklistEditorState()
        updateSelectionStateIfContent { state, _ ->
            state.copy(
                isSelectionModeActive = true,
                selectedEntryIds = state.selectedEntryIds + initialEntryId,
                showActionSheet = false,
            )
        }
    }

    private fun exitSelectionMode() {
        selectionState.value = ListDetailsSelectionState()
    }

    private fun startEditingChecklistItem(entryId: String) {
        val entry = contentState.value?.listContent?.entries?.filterIsInstance<ChecklistEntry>()
            ?.firstOrNull { it.id == entryId }
            ?: return
        selectionState.value = ListDetailsSelectionState()
        checklistEditorState.value = ChecklistEditorState(
            editingEntryId = entry.id,
            draftName = entry.itemName,
        )
    }

    private fun updateChecklistDraftName(value: String) {
        checklistEditorState.update { state ->
            state.copy(draftName = value)
        }
    }

    private fun saveChecklistDraft() {
        val currentState = contentState.value ?: return
        if (currentState.listContent.listType != ListType.CHECKLIST) return

        val editorState = checklistEditorState.value
        val draftName = editorState.draftName.trim()
        if (draftName.isBlank()) {
            showError("Name cannot be empty")
            return
        }

        val now = Date()
        val existingEntry = currentState.listContent.entries
            .filterIsInstance<ChecklistEntry>()
            .firstOrNull { it.id == editorState.editingEntryId }

        val draft = existingEntry?.copy(
            itemName = draftName,
            lastUpdated = now,
        ) ?: ChecklistEntry(
            id = UUID.randomUUID().toString(),
            familyId = currentState.familyId,
            listId = listId,
            itemName = draftName,
            isChecked = false,
            lastUpdated = now,
            dateCreated = now,
        )

        viewModelScope.launch {
            val result = if (existingEntry == null) {
                userListRepository.saveChecklistEntry(draft).mapUnitSuccess()
            } else {
                userListRepository.updateChecklistEntry(draft)
            }

            when (result) {
                is Result.Success -> clearChecklistEditorState()
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun toggleEntrySelection(entryId: String) {
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

    private fun toggleAllEntrySelection() {
        updateSelectionStateIfContent { state, content ->
            val allEntryIds = content.listContent.entries.map { it.id }.toSet()
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

    private fun requestDeleteSelected() {
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

    private fun requestRenameList() {
        val list = currentList ?: return
        updateSelectionStateIfContent { state, _ ->
            state.copy(
                showActionSheet = false,
                showRenameListDialog = true,
                renameListText = list.itemName,
            )
        }
    }

    private fun dismissRenameListDialog() {
        updateSelectionStateIfContent { state, _ ->
            state.copy(
                showRenameListDialog = false,
                renameListText = "",
            )
        }
    }

    private fun updateRenameListText(value: String) {
        updateSelectionStateIfContent { state, _ ->
            state.copy(renameListText = value)
        }
    }

    private fun confirmRenameList() {
        val current = currentList ?: return
        val newName = selectionState.value.renameListText.trim()
        if (newName.isBlank()) {
            showError("List name cannot be empty")
            return
        }
        if (newName == current.itemName) {
            showError("List already called $newName")
            return
        }

        val updatedList = current.copy(
            itemName = newName,
            lastUpdated = Date(),
        )

        viewModelScope.launch {
            when (val result = userListRepository.updateUserList(updatedList)) {
                is Result.Success -> dismissRenameListDialog()
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun dismissDeleteSelectedDialog() {
        updateSelectionStateIfContent { state, _ ->
            state.copy(showDeleteSelectedDialog = false)
        }
    }

    private fun confirmDeleteSelected() {
        val currentState = contentState.value ?: return
        val selectedEntryIds = selectionState.value.selectedEntryIds
        val selectedEntries = currentState.listContent.entries.filter { it.id in selectedEntryIds }
        if (selectedEntries.isEmpty()) {
            dismissDeleteSelectedDialog()
            return
        }

        viewModelScope.launch {
            val result = when (currentState.listContent.listType) {
                ListType.ROUTINE -> deleteRoutineListEntriesUseCase(selectedEntries.filterIsInstance<RoutineListEntry>())
                ListType.WISH_LIST -> userListRepository.deleteWishListEntries(selectedEntries.map { it.id })
                ListType.NOTES -> userListRepository.deleteNoteEntries(selectedEntries.map { it.id })
                ListType.CHECKLIST -> userListRepository.deleteChecklistEntries(selectedEntries.map { it.id })
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

    private fun toggleEntryCompleted(entryId: String) {
        val entry = contentState.value?.listContent?.entries?.firstOrNull { it.id == entryId } ?: return
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
                currentList = list
                if (list == null) {
                    flowOf(null)
                } else {
                    observeListEntries(context.familyId, list).flatMapLatest { entries ->
                        when (list.type) {
                            ListType.ROUTINE -> observeRoutineImageBitmaps(entries.filterIsInstance<RoutineListEntry>())
                                .map { imageBitmaps ->
                                    list.toContentSnapshot(entries, imageBitmaps)
                                }

                            else -> flowOf(list.toContentSnapshot(entries, emptyMap()))
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
        val listContent = when (type) {
            ListType.ROUTINE -> ListDetailsListContent.Routines(
                listName = itemName,
                entries = entries.filterIsInstance<RoutineListEntry>(),
                imageBitmaps = imageBitmaps,
            )

            ListType.WISH_LIST -> ListDetailsListContent.Wishes(
                listName = itemName,
                entries = entries.filterIsInstance<WishListEntry>(),
            )

            ListType.NOTES -> ListDetailsListContent.Notes(
                listName = itemName,
                entries = entries.filterIsInstance<NoteEntry>(),
            )

            ListType.CHECKLIST -> ListDetailsListContent.CheckItems(
                listName = itemName,
                entries = entries.filterIsInstance<ChecklistEntry>(),
            )
        }
        return ListDetailsContentSnapshot(
            familyId = familyId,
            listContent = listContent,
        )
    }

    private fun ListDetailsContentSnapshot.toUiState(
        selectionState: ListDetailsSelectionState,
        checklistEditorState: ChecklistEditorState,
    ): ListDetailsUiState.Content {
        val validEntryIds = listContent.entries.map { it.id }.toSet()
        val selectedEntryIds = selectionState.selectedEntryIds.intersect(validEntryIds)
        val resolvedChecklistEditorState = if (
            listContent.listType == ListType.CHECKLIST &&
            checklistEditorState.editingEntryId != null &&
            checklistEditorState.editingEntryId !in validEntryIds
        ) {
            ChecklistEditorState()
        } else {
            checklistEditorState
        }

        return ListDetailsUiState.Content(
            familyId = familyId,
            listContent = listContent,
            isSelectionMode = selectionState.isSelectionModeActive && validEntryIds.isNotEmpty(),
            selectedEntryIds = selectedEntryIds,
            checklistEditorState = resolvedChecklistEditorState,
            isAllEntriesSelected = validEntryIds.isNotEmpty() && selectedEntryIds.size == validEntryIds.size,
            showActionSheet = selectionState.showActionSheet,
            showRenameListDialog = selectionState.showRenameListDialog,
            renameListText = selectionState.renameListText,
            showDeleteSelectedDialog = selectionState.showDeleteSelectedDialog && selectedEntryIds.isNotEmpty(),
        )
    }

    private data class SessionContext(
        val uid: String,
        val familyId: String,
    )

    private data class ListDetailsContentSnapshot(
        val familyId: String,
        val listContent: ListDetailsListContent,
    )

    private data class ListDetailsSelectionState(
        val isSelectionModeActive: Boolean = false,
        val selectedEntryIds: Set<String> = emptySet(),
        val showActionSheet: Boolean = false,
        val showRenameListDialog: Boolean = false,
        val renameListText: String = "",
        val showDeleteSelectedDialog: Boolean = false,
    )

    private fun clearChecklistEditorState() {
        checklistEditorState.value = ChecklistEditorState()
    }

    private fun <T> Result<T, AppError>.mapUnitSuccess(): Result<Unit, AppError> {
        return when (this) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(error)
        }
    }
}
