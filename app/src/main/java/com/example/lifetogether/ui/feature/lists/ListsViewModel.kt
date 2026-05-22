package com.example.lifetogether.ui.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userListRepository: UserListRepository,
) : ViewModel() {
    private var familyId: String? = null
    private var uid: String? = null
    private var listsJob: Job? = null

    private val _uiState = MutableStateFlow<ListsUiState>(ListsUiState.Loading)
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<ListsCommand>(Channel.BUFFERED)
    val commands: Flow<ListsCommand> = _commands.receiveAsFlow()

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
                    setUpLists()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                    _uiState.value = ListsUiState.Loading
                }
            }
        }
    }

    fun onEvent(event: ListsUiEvent) {
        when (event) {
            ListsUiEvent.CreateListClicked -> openCreateDialog()
            ListsUiEvent.DismissDialog -> updateContentState { it.copy(dialog = null) }
            is ListsUiEvent.CreateListNameChanged -> updateContentState { state ->
                state.copy(dialog = (state.dialog as? ListsDialogState.CreateList)?.copy(name = event.value))
            }
            is ListsUiEvent.CreateListTypeChanged -> updateContentState { state ->
                state.copy(dialog = (state.dialog as? ListsDialogState.CreateList)?.copy(type = event.value))
            }
            is ListsUiEvent.CreateListVisibilityChanged -> updateContentState { state ->
                state.copy(dialog = (state.dialog as? ListsDialogState.CreateList)?.copy(visibility = event.value))
            }
            ListsUiEvent.ConfirmCreateListClicked -> createList()
            ListsUiEvent.ToggleActionSheet -> updateContentState { it.copy(showActionSheet = !it.showActionSheet) }
            ListsUiEvent.StartSelectionMode -> updateContentState { it.copy(isSelectionMode = true, showActionSheet = false) }
            is ListsUiEvent.EnterSelectionMode -> updateContentState {
                it.copy(isSelectionMode = true, selectedListIds = setOf(event.listId))
            }
            ListsUiEvent.ExitSelectionMode -> updateContentState {
                it.copy(isSelectionMode = false, selectedListIds = emptySet(), isAllSelected = false)
            }
            is ListsUiEvent.ToggleListSelection -> updateContentState { state ->
                val updated = if (state.selectedListIds.contains(event.listId)) {
                    state.selectedListIds - event.listId
                } else {
                    state.selectedListIds + event.listId
                }
                state.copy(selectedListIds = updated, isAllSelected = updated.size == state.userLists.size)
            }
            ListsUiEvent.ToggleAllListSelection -> updateContentState { state ->
                if (state.isAllSelected) {
                    state.copy(selectedListIds = emptySet(), isAllSelected = false)
                } else {
                    state.copy(selectedListIds = state.userLists.map { it.id }.toSet(), isAllSelected = true)
                }
            }
            ListsUiEvent.ConfirmDeleteSelected -> deleteSelectedLists()
        }
    }

    private fun setUpLists() {
        val familyIdValue = familyId ?: return

        listsJob?.cancel()
        listsJob = viewModelScope.launch {
            userListRepository.observeUserLists(familyId = familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        updateContentState { state ->
                            state.copy(
                                userLists = result.data.sortedBy { it.itemName.lowercase() },
                            )
                        }
                        if (_uiState.value is ListsUiState.Loading) {
                            _uiState.value = ListsUiState.Content(
                                userLists = result.data.sortedBy { it.itemName.lowercase() },
                            )
                        }
                    }

                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun openCreateDialog() {
        updateContentState {
            it.copy(
                dialog = ListsDialogState.CreateList(),
                isSaving = false,
            )
        }
    }

    private fun createList() {
        val activeFamilyId = familyId
        val activeUid = uid
        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) return

        val currentState = currentContentState() ?: return
        val createDialog = currentState.dialog as? ListsDialogState.CreateList ?: return
        if (createDialog.name.isBlank()) {
            showError("Name cannot be empty")
            return
        }

        updateContentState { it.copy(isSaving = true) }
        viewModelScope.launch {
            val list = UserList(
                id = UUID.randomUUID().toString(),
                familyId = activeFamilyId,
                itemName = createDialog.name.trim(),
                lastUpdated = Date(),
                dateCreated = Date(),
                type = createDialog.type,
                visibility = createDialog.visibility,
                ownerUid = activeUid,
            )
            when (val result = userListRepository.saveUserList(list)) {
                is Result.Success -> {
                    updateContentState { it.copy(dialog = null, isSaving = false) }
                    _commands.send(ListsCommand.NavigateToListDetails(result.data))
                }

                is Result.Failure -> {
                    updateContentState { it.copy(isSaving = false) }
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun deleteSelectedLists() {
        val ids = currentContentState()?.selectedListIds ?: return
        updateContentState { it.copy(
            showActionSheet = false,
            isSelectionMode = false,
            selectedListIds = emptySet(),
            isAllSelected = false
        ) }
        viewModelScope.launch {
            ids.forEach { listId ->
                val result = userListRepository.deleteUserList(listId)
                if (result is Result.Failure) showError(result.error.toUserMessage())
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

    private fun updateContentState(transform: (ListsUiState.Content) -> ListsUiState.Content) {
        _uiState.update { state ->
            val contentState = state as? ListsUiState.Content ?: return@update state
            transform(contentState)
        }
    }

    private fun currentContentState(): ListsUiState.Content? {
        return _uiState.value as? ListsUiState.Content
    }
}
