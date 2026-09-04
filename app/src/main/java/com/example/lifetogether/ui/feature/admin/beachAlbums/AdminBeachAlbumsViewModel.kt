package com.example.lifetogether.ui.feature.admin.beachAlbums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.BeachRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminBeachAlbumsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val beachRepository: BeachRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminBeachAlbumsUiState>(AdminBeachAlbumsUiState.Loading)
    val uiState: StateFlow<AdminBeachAlbumsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands = _uiCommands.receiveAsFlow()

    private var observeJob: Job? = null
    private var familyId: String? = null

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (!newFamilyId.isNullOrBlank() && newFamilyId != familyId) {
                    familyId = newFamilyId
                    observeAlbums(newFamilyId)
                }
            }
        }
    }

    private fun observeAlbums(fId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                beachRepository.observeBeachAlbums(fId),
                beachRepository.observeBeachAlbumThumbnails(fId),
            ) { albumsResult, thumbnails ->
                when (albumsResult) {
                    is Result.Success -> {
                        val current = _uiState.value
                        val currentContent = current as? AdminBeachAlbumsUiState.Content
                        _uiState.value = AdminBeachAlbumsUiState.Content(
                            albums = albumsResult.data,
                            thumbnails = thumbnails,
                            dialogState = currentContent?.dialogState,
                            isRefreshing = currentContent?.isRefreshing ?: false,
                            isSyncing = currentContent?.isSyncing ?: false,
                        )
                    }
                    is Result.Failure -> {
                        _uiCommands.send(UiCommand.ShowSnackbar(albumsResult.error.toString()))
                    }
                }
            }.collect {}
        }
    }

    fun onEvent(event: AdminBeachAlbumsUiEvent) {
        when (event) {
            is AdminBeachAlbumsUiEvent.RefreshAlbums -> refreshAlbums()
            is AdminBeachAlbumsUiEvent.OpenCreateDialog -> {
                updateContent { it.copy(dialogState = BeachAlbumsDialogState.CreateAlbum()) }
            }
            is AdminBeachAlbumsUiEvent.NameDraftChanged -> {
                updateContent { content ->
                    val createDialog = content.dialogState as? BeachAlbumsDialogState.CreateAlbum
                        ?: return@updateContent content
                    content.copy(dialogState = createDialog.copy(nameDraft = event.value))
                }
            }
            is AdminBeachAlbumsUiEvent.ConfirmCreateAlbum -> {
                val currentFamilyId = familyId ?: return
                val content = _uiState.value as? AdminBeachAlbumsUiState.Content ?: return
                val createDialog = content.dialogState as? BeachAlbumsDialogState.CreateAlbum ?: return
                val name = createDialog.nameDraft.trim()
                if (name.isBlank()) return

                updateContent { it.copy(dialogState = null) }
                viewModelScope.launch {
                    when (val result = beachRepository.createBeachAlbum(currentFamilyId, name)) {
                        is Result.Success -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Beach album '$name' created", severity = SnackbarSeverity.Info))
                        }
                        is Result.Failure -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Failed to create album: ${result.error}"))
                        }
                    }
                }
            }
            is AdminBeachAlbumsUiEvent.DismissDialog -> {
                updateContent { it.copy(dialogState = null) }
            }
        }
    }

    private fun refreshAlbums() {
        val fId = familyId ?: return
        updateContent { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            beachRepository.syncBeachAlbumsFromRemote(fId).collect { result ->
                if (result is Result.Failure) {
                    _uiCommands.send(UiCommand.ShowSnackbar("Refresh failed: ${result.error}"))
                }
            }
            updateContent { it.copy(isRefreshing = false) }
        }
    }

    private inline fun updateContent(transform: (AdminBeachAlbumsUiState.Content) -> AdminBeachAlbumsUiState.Content) {
        val current = _uiState.value as? AdminBeachAlbumsUiState.Content ?: return
        _uiState.value = transform(current)
    }
}
