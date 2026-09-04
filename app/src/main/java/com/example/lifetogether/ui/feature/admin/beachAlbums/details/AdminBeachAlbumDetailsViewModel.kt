package com.example.lifetogether.ui.feature.admin.beachAlbums.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.BeachRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AdminBeachAlbumDetailsViewModel.Factory::class)
class AdminBeachAlbumDetailsViewModel @AssistedInject constructor(
    @Assisted val albumId: String,
    private val sessionRepository: SessionRepository,
    private val beachRepository: BeachRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AdminBeachAlbumDetailsViewModel
    }

    private val _uiState = MutableStateFlow<AdminBeachAlbumDetailsUiState>(AdminBeachAlbumDetailsUiState.Loading)
    val uiState: StateFlow<AdminBeachAlbumDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands = _uiCommands.receiveAsFlow()

    private val _featureCommands = Channel<AdminBeachAlbumDetailsCommand>(Channel.BUFFERED)
    val featureCommands = _featureCommands.receiveAsFlow()

    private var observeAlbumJob: Job? = null
    private var syncRemoteJob: Job? = null
    private var familyId: String? = null

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (!newFamilyId.isNullOrBlank() && newFamilyId != familyId) {
                    familyId = newFamilyId
                    observeAlbumAndMedia(newFamilyId)
                    syncRemoteMedia(newFamilyId)
                }
            }
        }
    }

    private fun observeAlbumAndMedia(fId: String) {
        observeAlbumJob?.cancel()
        observeAlbumJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                beachRepository.observeBeachAlbumById(fId, albumId),
                beachRepository.observeBeachMedia(fId, albumId),
            ) { albumResult, mediaResult ->
                when (albumResult) {
                    is Result.Success -> {
                        val album = albumResult.data
                        val mediaList = when (mediaResult) {
                            is Result.Success -> mediaResult.data
                            is Result.Failure -> emptyList()
                        }
                        val incompleteCount = mediaList.count { it.downloadState != com.example.lifetogether.domain.model.gallery.MediaDownloadState.READY }
                        val isPartialLoad = album.count > mediaList.size || incompleteCount > 0
                        val isSyncing = incompleteCount > 0 || (album.count > 0 && mediaList.isEmpty())

                        val currentContent = _uiState.value as? AdminBeachAlbumDetailsUiState.Content
                        _uiState.value = currentContent?.copy(
                            album = album,
                            mediaList = mediaList,
                            isSyncing = isSyncing,
                            isPartialLoad = isPartialLoad,
                            isRefreshing = false,
                        ) ?: AdminBeachAlbumDetailsUiState.Content(
                            album = album,
                            mediaList = mediaList,
                            isSyncing = isSyncing,
                            isPartialLoad = isPartialLoad,
                            isRefreshing = false,
                        )
                    }
                    is Result.Failure -> {
                        if (albumResult.error is AppError.NotFound) {
                            _featureCommands.send(AdminBeachAlbumDetailsCommand.NavigateBack)
                        } else {
                            _uiCommands.send(UiCommand.ShowSnackbar(albumResult.error.toString()))
                        }
                    }
                }
            }.collect {}
        }
    }

    private fun syncRemoteMedia(fId: String) {
        syncRemoteJob?.cancel()
        syncRemoteJob = viewModelScope.launch {
            beachRepository.syncBeachMediaFromRemote(fId, albumId).collect {}
        }
    }

    fun onEvent(event: AdminBeachAlbumDetailsUiEvent) {
        when (event) {
            is AdminBeachAlbumDetailsUiEvent.RetryFetchBeachMedia -> {
                val fId = familyId ?: return
                updateContent { it.copy(isRefreshing = true) }
                syncRemoteMedia(fId)
            }
            is AdminBeachAlbumDetailsUiEvent.RetryMediaDownload -> {
                val fId = familyId ?: return
                viewModelScope.launch {
                    updateContent { it.copy(retryingMediaIds = it.retryingMediaIds + event.mediaId, isSyncing = true) }
                    beachRepository.retryBeachMediaDownloads(listOf(event.mediaId), fId)
                    updateContent { it.copy(retryingMediaIds = it.retryingMediaIds - event.mediaId) }
                }
            }
            is AdminBeachAlbumDetailsUiEvent.ToggleSelectionMode -> {
                val content = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
                val newSelectionMode = !content.isSelectionMode
                _uiState.value = content.copy(
                    isSelectionMode = newSelectionMode,
                    selectedMediaIds = if (newSelectionMode) content.selectedMediaIds else emptySet(),
                )
            }
            is AdminBeachAlbumDetailsUiEvent.ToggleMoreActions -> {
                updateContent { it.copy(showMoreActions = !it.showMoreActions) }
            }
            is AdminBeachAlbumDetailsUiEvent.OpenQrDialog -> {
                updateContent { it.copy(showMoreActions = false, dialogState = BeachAlbumDetailsDialogState.QrCode) }
            }
            is AdminBeachAlbumDetailsUiEvent.OpenRenameAlbumDialog -> {
                val content = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
                updateContent {
                    it.copy(
                        showMoreActions = false,
                        dialogState = BeachAlbumDetailsDialogState.RenameAlbum(nameDraft = content.album.name),
                    )
                }
            }
            is AdminBeachAlbumDetailsUiEvent.RenameDraftChanged -> {
                updateContent { content ->
                    val renameState = content.dialogState as? BeachAlbumDetailsDialogState.RenameAlbum
                        ?: return@updateContent content
                    content.copy(dialogState = renameState.copy(nameDraft = event.value))
                }
            }
            is AdminBeachAlbumDetailsUiEvent.ConfirmRenameAlbum -> {
                val content = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
                val renameState = content.dialogState as? BeachAlbumDetailsDialogState.RenameAlbum ?: return
                val newName = renameState.nameDraft.trim()
                if (newName.isBlank()) return
                updateContent { it.copy(dialogState = null) }
                viewModelScope.launch {
                    val updatedAlbum = content.album.copy(name = newName)
                    when (val result = beachRepository.updateBeachAlbum(updatedAlbum)) {
                        is Result.Success -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Renamed beach album to '$newName'", severity = SnackbarSeverity.Info))
                        }
                        is Result.Failure -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Failed to rename album: ${result.error}"))
                        }
                    }
                }
            }
            is AdminBeachAlbumDetailsUiEvent.OpenDeleteAlbumDialog -> {
                updateContent { it.copy(showMoreActions = false, dialogState = BeachAlbumDetailsDialogState.DeleteAlbum) }
            }
            is AdminBeachAlbumDetailsUiEvent.ConfirmDeleteAlbum -> {
                val currentFamilyId = familyId ?: return
                updateContent { it.copy(dialogState = null) }
                observeAlbumJob?.cancel()
                observeAlbumJob = null
                viewModelScope.launch {
                    when (val result = beachRepository.deleteBeachAlbum(currentFamilyId, albumId)) {
                        is Result.Success -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Beach album deleted"))
                            _featureCommands.send(AdminBeachAlbumDetailsCommand.NavigateBack)
                        }
                        is Result.Failure -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Failed to delete album: ${result.error}"))
                        }
                    }
                }
            }
            is AdminBeachAlbumDetailsUiEvent.MediaClicked -> {
                val content = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
                val media = content.mediaList.find { it.id == event.mediaId }
                if (content.isSelectionMode) {
                    val updated = content.selectedMediaIds.toMutableSet()
                    if (updated.contains(event.mediaId)) {
                        updated.remove(event.mediaId)
                    } else {
                        updated.add(event.mediaId)
                    }
                    _uiState.value = content.copy(
                        selectedMediaIds = updated,
                        isSelectionMode = updated.isNotEmpty(),
                    )
                } else if (media != null && media.downloadState == com.example.lifetogether.domain.model.gallery.MediaDownloadState.FAILED) {
                    onEvent(AdminBeachAlbumDetailsUiEvent.RetryMediaDownload(event.mediaId))
                }
            }
            is AdminBeachAlbumDetailsUiEvent.MediaLongClicked -> {
                val content = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
                val updated = content.selectedMediaIds.toMutableSet()
                if (updated.contains(event.mediaId)) {
                    updated.remove(event.mediaId)
                } else {
                    updated.add(event.mediaId)
                }
                _uiState.value = content.copy(
                    selectedMediaIds = updated,
                    isSelectionMode = updated.isNotEmpty(),
                )
            }
            is AdminBeachAlbumDetailsUiEvent.ToggleSelectAll -> {
                val content = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
                val allIds = content.mediaList.map { it.id }.toSet()
                val newSelection = if (content.selectedMediaIds.size == allIds.size) emptySet() else allIds
                _uiState.value = content.copy(
                    selectedMediaIds = newSelection,
                    isSelectionMode = newSelection.isNotEmpty(),
                )
            }
            is AdminBeachAlbumDetailsUiEvent.ClearSelection -> {
                updateContent { it.copy(selectedMediaIds = emptySet(), isSelectionMode = false) }
            }
            is AdminBeachAlbumDetailsUiEvent.OpenDeleteMediaDialog -> {
                updateContent { it.copy(dialogState = BeachAlbumDetailsDialogState.DeleteSelectedMedia) }
            }
            is AdminBeachAlbumDetailsUiEvent.ConfirmDeleteSelectedMedia -> {
                val currentFamilyId = familyId ?: return
                val content = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
                val selectedIds = content.selectedMediaIds.toList()
                if (selectedIds.isEmpty()) return
                updateContent { it.copy(dialogState = null, selectedMediaIds = emptySet(), isSelectionMode = false) }
                viewModelScope.launch {
                    when (val result = beachRepository.deleteBeachMedia(currentFamilyId, selectedIds, albumId)) {
                        is Result.Success -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Deleted selected photos"))
                        }
                        is Result.Failure -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Failed to delete photos: ${result.error}"))
                        }
                    }
                }
            }
            is AdminBeachAlbumDetailsUiEvent.DismissDialog -> {
                updateContent { it.copy(dialogState = null) }
            }
            is AdminBeachAlbumDetailsUiEvent.UploadPhotos -> {
                val currentFamilyId = familyId ?: return
                val uris = event.uris
                if (uris.isEmpty()) return
                viewModelScope.launch {
                    _uiState.value = (_uiState.value as? AdminBeachAlbumDetailsUiState.Content)?.copy(
                        uploadProgress = UploadProgress(0, uris.size)
                    ) ?: _uiState.value

                    val result = beachRepository.uploadBeachMedia(currentFamilyId, albumId, uris) { current, total ->
                        updateContent { it.copy(uploadProgress = UploadProgress(current, total)) }
                    }

                    updateContent { it.copy(uploadProgress = null) }
                    when (result) {
                        is Result.Success -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Uploaded ${uris.size} photo(s)", severity = SnackbarSeverity.Info))
                        }
                        is Result.Failure -> {
                            _uiCommands.send(UiCommand.ShowSnackbar("Upload failed: ${result.error}"))
                        }
                    }
                }
            }
        }
    }

    private inline fun updateContent(transform: (AdminBeachAlbumDetailsUiState.Content) -> AdminBeachAlbumDetailsUiState.Content) {
        val current = _uiState.value as? AdminBeachAlbumDetailsUiState.Content ?: return
        _uiState.value = transform(current)
    }
}
