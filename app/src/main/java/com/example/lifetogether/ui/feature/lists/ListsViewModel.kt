package com.example.lifetogether.ui.feature.lists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
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
            ListsUiEvent.CreateDialogDismissed -> dismissCreateDialog()
            is ListsUiEvent.CreateListNameChanged -> updateContentState { it.copy(newListName = event.value) }
            is ListsUiEvent.CreateListTypeChanged -> updateContentState { it.copy(newListType = event.value) }
            is ListsUiEvent.CreateListVisibilityChanged -> updateContentState { it.copy(newListVisibility = event.value) }
            ListsUiEvent.ConfirmCreateListClicked -> createList()
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
                showCreateDialog = true,
                newListName = "",
                newListType = ListType.ROUTINE,
                newListVisibility = Visibility.PRIVATE,
                isSaving = false,
            )
        }
    }

    private fun dismissCreateDialog() {
        updateContentState { it.copy(showCreateDialog = false) }
    }

    private fun createList() {
        val activeFamilyId = familyId
        val activeUid = uid
        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) return

        val currentState = currentContentState() ?: return
        if (currentState.newListName.isBlank()) {
            showError("Name cannot be empty")
            return
        }

        updateContentState { it.copy(isSaving = true) }
        viewModelScope.launch {
            val list = UserList(
                familyId = activeFamilyId,
                itemName = currentState.newListName.trim(),
                lastUpdated = Date(),
                dateCreated = Date(),
                type = currentState.newListType,
                visibility = currentState.newListVisibility,
                ownerUid = activeUid,
            )
            when (val result = userListRepository.saveUserList(list)) {
                is Result.Success -> {
                    updateContentState { it.copy(showCreateDialog = false, isSaving = false) }
                    _commands.send(ListsCommand.NavigateToListDetails(result.data))
                }

                is Result.Failure -> {
                    updateContentState { it.copy(isSaving = false) }
                    showError(result.error.toUserMessage())
                }
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
