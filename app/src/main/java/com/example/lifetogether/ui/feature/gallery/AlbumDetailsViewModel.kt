package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.gallery.DeleteAlbumUseCase
import com.example.lifetogether.domain.usecase.gallery.DeleteMediaUseCase
import com.example.lifetogether.domain.usecase.gallery.GetAlbumDisplayModelsUseCase
import com.example.lifetogether.domain.usecase.item.MoveMediaToAlbumUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.model.MenuAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val galleryRepository: GalleryRepository,
    private val moveMediaToAlbumUseCase: MoveMediaToAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val getAlbumDisplayModelsUseCase: GetAlbumDisplayModelsUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumDetailsUiState())
    val uiState: StateFlow<AlbumDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<AlbumDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<AlbumDetailsCommand> = _commands.receiveAsFlow()

    private val requestedThumbnailIds = mutableSetOf<String>()
    private var familyId: String? = null
    private var albumId: String? = null

    private var syncRetryAttempts = 0
    private val maxSyncRetryAttempts = 3

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    _uiState.update { it.copy(familyId = familyId) }
                    albumId?.let { startFetch() }
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                }
            }
        }
    }

    fun setUp(addedAlbumId: String) {
        if (albumId == addedAlbumId && familyId != null) return

        albumId = addedAlbumId
        syncRetryAttempts = 0

        if (familyId != null) {
            _uiState.update { it.copy(isSyncing = false) }
            startFetch()
        }
    }

    fun onEvent(event: AlbumDetailsUiEvent) {
        when (event) {
            AlbumDetailsUiEvent.RetryFetchAlbumMedia -> retryFetchAlbumMedia()
            AlbumDetailsUiEvent.ToggleOverflowMenu -> toggleOverflowMenu()
            AlbumDetailsUiEvent.ToggleSelectionMode -> toggleSelectionMode()
            AlbumDetailsUiEvent.ToggleAllMediaSelection -> toggleAllMediaSelection()
            is AlbumDetailsUiEvent.ToggleMediaSelection -> toggleMediaSelection(event.mediaId)
            is AlbumDetailsUiEvent.EnterSelectionMode -> enterSelectionMode(event.mediaId)
            AlbumDetailsUiEvent.RequestImageUpload -> Unit
            AlbumDetailsUiEvent.DismissImageUploadDialog -> Unit
            AlbumDetailsUiEvent.ConfirmImageUploadDialog -> Unit
            is AlbumDetailsUiEvent.StartOverflowAction -> startOverflowAction(event.action)
            AlbumDetailsUiEvent.DismissOverflowMenuActionDialog -> dismissOverflowMenuActionDialog()
            is AlbumDetailsUiEvent.SetActionDialogText -> setActionDialogText(event.text)
            AlbumDetailsUiEvent.ConfirmRenameAlbum -> renameAlbum()
            AlbumDetailsUiEvent.ConfirmDeleteAlbum -> deleteAlbum()
            AlbumDetailsUiEvent.DownloadSelectedMedia -> downloadSelectedMedia()
            AlbumDetailsUiEvent.ConfirmDeleteSelectedMedia -> deleteSelectedMedia()
            is AlbumDetailsUiEvent.MoveSelectedMediaToAlbum -> moveSelectedMediaToAlbum(event.albumId)
            AlbumDetailsUiEvent.ConfirmMoveSelectedMedia -> showError("Please choose an album first")
        }
    }

    private fun startFetch() {
        observeAlbum()
        observeAlbumMedia()
    }

    private fun observeAlbum() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        viewModelScope.launch {
            galleryRepository.observeAlbumById(familyIdValue, albumIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val album = result.data
                        _uiState.update { state ->
                            state.copy(
                                album = album,
                                isSyncing = album.count > 0 && state.media.isEmpty(),
                            )
                        }
                    }

                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun observeAlbumMedia() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        viewModelScope.launch {
            val expectedCount = _uiState.value.album?.count ?: 0
            if (expectedCount > 0 && _uiState.value.media.isEmpty()) {
                _uiState.update { it.copy(isSyncing = true) }
            }

            galleryRepository.observeAlbumMedia(familyIdValue, albumIdValue).collect { result ->
                when (result) {
                    is Result.Success -> handleMediaSuccess(result.data)
                    is Result.Failure -> handleMediaFailure(result.error.toUserMessage())
                }
            }
        }
    }

    private fun handleMediaSuccess(items: List<GalleryMedia>) {
        if (items.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    media = items,
                    isSyncing = false,
                )
            }
            groupMedia()
            items.forEach { media ->
                val mediaId = media.id ?: return@forEach
                if (!requestedThumbnailIds.contains(mediaId)) {
                    requestedThumbnailIds.add(mediaId)
                    fetchThumbnail(mediaId)
                }
            }

            val expectedCount = _uiState.value.album?.count ?: 0
            if (expectedCount > items.size && syncRetryAttempts < maxSyncRetryAttempts) {
                _uiState.update { it.copy(isSyncing = true, isPartialLoad = true) }
                syncRetryAttempts += 1
                viewModelScope.launch {
                    delay(3000)
                    observeAlbumMedia()
                }
                return
            } else if (expectedCount > items.size) {
                _uiState.update { it.copy(isPartialLoad = true, isSyncing = false, isRefreshing = false) }
                return
            }

            syncRetryAttempts = 0
            _uiState.update { it.copy(isPartialLoad = false, isRefreshing = false) }
            return
        }

        val expectedCount = _uiState.value.album?.count ?: 0
        if (expectedCount > 0 && syncRetryAttempts < maxSyncRetryAttempts) {
            _uiState.update { it.copy(isSyncing = true) }
            syncRetryAttempts += 1
            viewModelScope.launch {
                delay(2000)
                observeAlbumMedia()
            }
        } else {
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    private fun groupMedia() {
        val grouped = uiState.value.media
            .sortedByDescending { it.dateCreated }
            .groupBy { it.dateCreated?.toFullDateString() ?: "Unknown Date" }
            .toList()

        _uiState.update { it.copy(groupedMedia = grouped) }
    }

    private fun handleMediaFailure(message: String) {
        _uiState.update { it.copy(isSyncing = false, isRefreshing = false) }
        showError(message)
    }

    private fun retryFetchAlbumMedia() {
        _uiState.update { it.copy(isRefreshing = true) }
        syncRetryAttempts = 0
        observeAlbumMedia()
    }

    private fun fetchThumbnail(mediaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = galleryRepository.getAlbumMediaThumbnail(mediaId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(thumbnails = state.thumbnails + (mediaId to result.data))
                    }
                }

                is Result.Failure -> Unit
            }
        }
    }

    private fun renameAlbum() {
        val newName = _uiState.value.actionDialogText.trim()
        val currentAlbum = _uiState.value.album

        if (newName.isEmpty()) {
            showError("Album name cannot be empty")
            return
        }
        if (newName == currentAlbum?.itemName) {
            showError("Album already called $newName")
            return
        }

        val updatedAlbum = currentAlbum?.copy(itemName = newName) ?: return

        viewModelScope.launch {
            when (val result = galleryRepository.updateAlbum(updatedAlbum)) {
                is Result.Success -> {
                    _uiState.update { it.copy(album = updatedAlbum) }
                    dismissOverflowMenuActionDialog()
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun deleteAlbum() {
        val albumIdValue = uiState.value.album?.id ?: return

        viewModelScope.launch {
            when (val result = deleteAlbumUseCase.invoke(albumIdValue, uiState.value.media)) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                    sendCommand(AlbumDetailsCommand.NavigateBack)
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun downloadSelectedMedia() {
        val familyIdValue = familyId
        val selectedMedia = _uiState.value.selectedMedia.toList()
        if (selectedMedia.isEmpty() || familyIdValue == null) {
            showError("Media data not available")
            return
        }

        viewModelScope.launch {
            galleryRepository.downloadMediaToGallery(
                mediaIds = selectedMedia,
                familyId = familyIdValue,
            ).collect { progress ->
                when (progress) {
                    is SaveProgress.Loading -> {
                        _uiState.update {
                            it.copy(
                                isDownloading = true,
                                downloadMessage = "Downloading ${progress.current} of ${progress.total}",
                            )
                        }
                    }

                    is SaveProgress.Finished -> {
                        if (progress.failureCount == 0) {
                            dismissOverflowMenuActionDialog()
                            val message = "Downloaded ${progress.successCount} item"
                            _uiState.update { it.copy(downloadMessage = message) }
                            delay(2000)
                            _uiState.update { it.copy(downloadMessage = null, isDownloading = false) }
                        } else {
                            showError("Failed to download media")
                        }
                    }

                    is SaveProgress.Error -> {
                        _uiState.update { it.copy(isDownloading = false, downloadMessage = null) }
                        showError(progress.message)
                    }
                }
            }
        }
    }

    private fun observeAlbums() {
        val familyIdValue = familyId ?: return

        viewModelScope.launch {
            getAlbumDisplayModelsUseCase.invoke(familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val possibleAlbums = result.data.filterNot { it.id == albumId }
                        _uiState.update { it.copy(albums = possibleAlbums, showOverflowMenuActionDialog = true) }

                        possibleAlbums.forEach { model ->
                            if (model.thumbnail == null && !requestedThumbnailIds.contains(model.id)) {
                                requestedThumbnailIds.add(model.id)
                                galleryRepository.fetchAlbumThumbnail(model.id)
                            }
                        }
                    }

                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun moveSelectedMediaToAlbum(newAlbumId: String) {
        val oldAlbumId = albumId ?: return
        val selectedMedia = _uiState.value.selectedMedia
        if (selectedMedia.isEmpty() || newAlbumId == _uiState.value.album?.id || newAlbumId.isEmpty()) return

        viewModelScope.launch {
            when (val result = moveMediaToAlbumUseCase.invoke(selectedMedia, newAlbumId, oldAlbumId)) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                    toggleSelectionMode()
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun deleteSelectedMedia() {
        val albumIdValue = uiState.value.album?.id ?: return
        val selectedMedia = uiState.value.media.filter { it.id in uiState.value.selectedMedia }

        if (selectedMedia.isEmpty()) return

        viewModelScope.launch {
            when (val result = deleteMediaUseCase.invoke(albumIdValue, selectedMedia)) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                    toggleSelectionMode()
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun toggleSelectionMode() {
        _uiState.update { state ->
            val newSelectionMode = !state.isSelectionModeActive
            state.copy(
                isSelectionModeActive = newSelectionMode,
                selectedMedia = if (newSelectionMode) state.selectedMedia else emptySet(),
                isAllMediaSelected = if (newSelectionMode) state.isAllMediaSelected else false,
            )
        }
    }

    private fun toggleOverflowMenu(show: Boolean? = null) {
        _uiState.update { it.copy(showOverflowMenu = show ?: !it.showOverflowMenu) }
    }

    private fun enterSelectionMode(mediaId: String?) {
        if (mediaId == null) return
        if (!_uiState.value.isSelectionModeActive) {
            toggleSelectionMode()
        }
        toggleMediaSelection(mediaId)
    }

    private fun toggleAllMediaSelection() {
        when (_uiState.value.isAllMediaSelected) {
            true -> {
                _uiState.update {
                    it.copy(
                        selectedMedia = emptySet(),
                        isAllMediaSelected = false,
                        isSelectionModeActive = false,
                    )
                }
            }

            false -> {
                _uiState.update { state ->
                    state.copy(
                        selectedMedia = state.media.mapNotNull { it.id }.toSet(),
                        isAllMediaSelected = true,
                    )
                }
            }
        }
    }

    private fun toggleMediaSelection(mediaId: String?) {
        if (mediaId == null) return

        if (_uiState.value.selectedMedia.contains(mediaId)) {
            _uiState.update { state ->
                state.copy(selectedMedia = state.selectedMedia - mediaId)
            }
        } else {
            _uiState.update { state ->
                state.copy(selectedMedia = state.selectedMedia + mediaId)
            }
        }
    }

    private fun startOverflowAction(action: MenuAction) {
        _uiState.update {
            it.copy(
                overflowMenuAction = action,
                showOverflowMenu = false,
            )
        }
        if (action == MenuAction.SelectionActions.MOVE) {
            observeAlbums()
        } else {
            _uiState.update { it.copy(showOverflowMenuActionDialog = true) }
        }
    }

    private fun dismissOverflowMenuActionDialog() {
        _uiState.update {
            it.copy(
                showOverflowMenuActionDialog = false,
                overflowMenuAction = null,
                actionDialogText = "",
            )
        }
    }

    private fun setActionDialogText(text: String) {
        _uiState.update { it.copy(actionDialogText = text) }
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

    private fun sendCommand(command: AlbumDetailsCommand) {
        viewModelScope.launch {
            _commands.send(command)
        }
    }
}
