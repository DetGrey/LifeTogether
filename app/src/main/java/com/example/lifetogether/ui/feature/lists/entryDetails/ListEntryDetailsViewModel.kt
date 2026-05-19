package com.example.lifetogether.ui.feature.lists.entryDetails

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.navigation.ListEntryDetailsNavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import androidx.navigation.toRoute

@HiltViewModel
class ListEntryDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val contentLoader: ListEntryDetailsLoader,
    private val formReducer: ListEntryDetailsFormReducer,
    private val entryDetailsSaver: ListEntryDetailsSaver,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    companion object {
        val WEEKDAYS: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }

    private val route = savedStateHandle.toRoute<ListEntryDetailsNavRoute>()
    private val listId: String = route.listId
    val entryId: String? = route.entryId

    private val _familyId = MutableStateFlow<String?>(null)
    val familyId = _familyId.asStateFlow()

    private var originalDetails: EntryDetailsContent? = null

    private val _uiState = MutableStateFlow<EntryDetailsUiState>(EntryDetailsUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<ListEntryDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<ListEntryDetailsCommand> = _commands.receiveAsFlow()

    init {
        viewModelScope.launch {
            contentLoader.observe(listId, entryId).collect { snapshot ->
                _familyId.value = snapshot.familyId
                when (val state = snapshot.state) {
                    ListEntryDetailsLoadState.Loading -> resetLoadingState()
                    is ListEntryDetailsLoadState.Content -> showContent(state.details, state.isNewEntry)
                    is ListEntryDetailsLoadState.Error -> showError(state.message)
                }
            }
        }
    }

    fun onUiEvent(event: ListEntryDetailsUiEvent) {
        when (event) {
            ListEntryDetailsUiEvent.EnterEditMode -> updateContent { it.copy(isEditing = true) }
            ListEntryDetailsUiEvent.RequestCancelEdit -> updateContent { it.copy(showDiscardDialog = true) }
            ListEntryDetailsUiEvent.ConfirmDiscard -> confirmDiscard()
            ListEntryDetailsUiEvent.DismissDiscardDialog -> updateContent { it.copy(showDiscardDialog = false) }
            ListEntryDetailsUiEvent.SaveClicked -> saveEntry()
            is ListEntryDetailsUiEvent.Routine.ImageSelected -> onImageSelected(event.uri)
            is ListEntryDetailsUiEvent.NameChanged,
            is ListEntryDetailsUiEvent.Routine.RecurrenceUnitChanged,
            is ListEntryDetailsUiEvent.Routine.IntervalChanged,
            is ListEntryDetailsUiEvent.Routine.SelectedWeekdaysChanged,
            is ListEntryDetailsUiEvent.Wish.PurchasedChanged,
            is ListEntryDetailsUiEvent.Wish.UrlChanged,
            is ListEntryDetailsUiEvent.Wish.PriceChanged,
            is ListEntryDetailsUiEvent.Wish.CurrencyCodeChanged,
            is ListEntryDetailsUiEvent.Wish.PriorityChanged,
            is ListEntryDetailsUiEvent.Wish.NotesChanged,
            is ListEntryDetailsUiEvent.Note.BodyChanged -> updateCurrentDetails { formReducer.reduce(it, event) }
        }
    }

    fun confirmDiscard() {
        val original = originalDetails ?: return
        updateContent {
            it.copy(
                details = original,
                isEditing = false,
                showDiscardDialog = false,
                isSaving = false,
            )
        }
    }

    fun saveEntry() {
        val content = currentContentState() ?: return showError("Entry is not ready yet")
        val activeFamilyId = _familyId.value ?: return showError("Missing family context")
        val now = Date()
        val shouldStayOnScreen = entryId != null && content.details is EntryDetailsContent.Routine

        updateContent { it.copy(isSaving = true) }

        viewModelScope.launch {
            val result = entryDetailsSaver.save(
                details = content.details,
                entryId = entryId,
                familyId = activeFamilyId,
                listId = listId,
                now = now,
                context = context,
            )

            updateContent { it.copy(isSaving = false) }

            when (result) {
                is Result.Success -> {
                    originalDetails = currentContentState()?.details
                    if (shouldStayOnScreen) {
                        updateContent { it.copy(isEditing = false) }
                    } else {
                        viewModelScope.launch {
                            _commands.send(ListEntryDetailsCommand.NavigateBack)
                        }
                    }
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun resetLoadingState() {
        originalDetails = null
        _uiState.value = EntryDetailsUiState.Loading
    }

    private fun showContent(details: EntryDetailsContent, isNewEntry: Boolean) {
        val currentState = _uiState.value
        if (currentState is EntryDetailsUiState.Content && currentState.isEditing && !currentState.isSaving) {
            return
        }

        originalDetails = details
        _uiState.update { current ->
            when (current) {
                is EntryDetailsUiState.Content -> current.copy(details = details)
                is EntryDetailsUiState.Loading -> EntryDetailsUiState.Content(
                    details = details,
                    isEditing = isNewEntry,
                    showDiscardDialog = false,
                    isSaving = false,
                )
            }
        }
    }

    private fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                uri.toBitmap(context.contentResolver)
            } ?: run {
                showError("Could not load the selected image")
                return@launch
            }

            updateCurrentDetails { details ->
                when (details) {
                    is EntryDetailsContent.Routine -> details.copy(
                        form = details.form.copy(
                            pendingImageUri = uri,
                            pendingImageBitmap = bitmap,
                        ),
                    )

                    else -> details
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

    private fun currentContentState(): EntryDetailsUiState.Content? {
        return _uiState.value as? EntryDetailsUiState.Content
    }

    private fun updateContent(block: (EntryDetailsUiState.Content) -> EntryDetailsUiState.Content) {
        _uiState.update { state ->
            if (state is EntryDetailsUiState.Content) block(state) else state
        }
    }

    private fun updateCurrentDetails(block: (EntryDetailsContent) -> EntryDetailsContent) {
        updateContent { state ->
            state.copy(details = block(state.details))
        }
    }
}
