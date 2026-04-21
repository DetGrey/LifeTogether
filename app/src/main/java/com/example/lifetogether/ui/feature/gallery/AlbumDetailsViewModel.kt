package com.example.lifetogether.ui.feature.gallery

import com.example.lifetogether.domain.result.toUserMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.usecase.gallery.DeleteAlbumUseCase
import com.example.lifetogether.domain.usecase.gallery.DeleteMediaUseCase
import com.example.lifetogether.domain.usecase.gallery.GetAlbumDisplayModelsUseCase
import com.example.lifetogether.domain.usecase.item.MoveMediaToAlbumUseCase
import com.example.lifetogether.ui.model.AlbumUiModel
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailsUiState(
    val album: Album? = null,
    val media: List<GalleryMedia> = emptyList(),
    val groupedMedia: List<Pair<String, List<GalleryMedia>>> = emptyList(),
    val thumbnails: Map<String, ByteArray> = emptyMap(),
    val isSyncing: Boolean = false,
    val showOverflowMenu: Boolean = false,
    val showOverflowMenuActionDialog: Boolean = false,
    val overflowMenuAction: MenuAction? = null,
    val actionDialogText: String = "",
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val isPartialLoad: Boolean = false, // True when some media failed to load
    val isRefreshing: Boolean = false, // User-triggered refresh
    val isSelectionModeActive: Boolean = false,
    val selectedMedia: Set<String> = emptySet(),
    val isAllMediaSelected: Boolean = false,
    val albums: List<AlbumUiModel> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadMessage: String? = null,
    val familyId: String? = null,
)

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

    private fun startFetch() {
        fetchAlbum()
        fetchAlbumMedia()
    }

    private fun fetchAlbum() {
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
    private fun fetchAlbumMedia() {
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

            // Detect partial downloads and trigger retry if needed
            val expectedCount = _uiState.value.album?.count ?: 0
            if (expectedCount > items.size && syncRetryAttempts < maxSyncRetryAttempts) {
                println("Partial media load detected: got ${items.size} of $expectedCount items, will retry")
                _uiState.update { it.copy(isSyncing = true, isPartialLoad = true) }
                syncRetryAttempts += 1
                viewModelScope.launch {
                    delay(3000) // Wait 3 seconds before retry to allow failed downloads to complete
                    fetchAlbumMedia()
                }
                return
            } else if (expectedCount > items.size) {
                // Max retries reached, mark as partial load permanently
                println("Partial media load detected but max retries reached: got ${items.size} of $expectedCount items")
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
                fetchAlbumMedia()
            }
        } else {
            _uiState.update { it.copy(isSyncing = false) }
        }
    }
    private fun groupMedia() {
        val grouped = uiState.value.media
            // 1. Sort everything by newest first
            .sortedByDescending { it.dateCreated }
            // 2. Group by the day (ignoring time)
            .groupBy { it.dateCreated?.toFullDateString() ?: "Unknown Date" }
            // 3. Convert to a list of pairs so the UI can iterate easily
            .toList()

        _uiState.update { it.copy(groupedMedia = grouped) }
    }
    private fun handleMediaFailure(message: String) {
        _uiState.update { it.copy(isSyncing = false, isRefreshing = false) }
        showError(message)
    }
    fun retryFetchAlbumMedia() {
        _uiState.update { it.copy(isRefreshing = true) }
        syncRetryAttempts = 0
        fetchAlbumMedia()
    }
    fun fetchThumbnail(mediaId: String) {
        if (_uiState.value.thumbnails.containsKey(mediaId)) return

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = galleryRepository.getAlbumMediaThumbnail(mediaId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(thumbnails = state.thumbnails + (mediaId to result.data))
                    }
                }
                is Result.Failure -> {
                    // Ignore missing thumbnail
                }
            }
        }
    }
    // ---------------------------------------------------------------- OVERFLOW ACTION FUNCTIONS
    // ---------------------------------- ALBUM OPTIONS
    fun renameAlbum() {
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
    fun deleteAlbum(onDeleteSuccess: () -> Unit) {
        val albumIdValue = uiState.value.album?.id ?: return

        viewModelScope.launch {
            when (val result = deleteAlbumUseCase.invoke(albumIdValue, uiState.value.media)) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                    onDeleteSuccess()
                }
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }
    // ---------------------------------- SELECTED MEDIA OPTIONS
    fun downloadSelectedMedia() {
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
                        _uiState.update { it.copy(isDownloading = true, downloadMessage = "Downloading ${progress.current} of ${progress.total}") }
                    }
                    is SaveProgress.Finished -> {
                        if (progress.failureCount == 0) {
                            dismissOverflowMenuActionDialog()
                            val message = if (progress.failureCount == 0) { "Downloaded ${progress.successCount} item" }
                            else { "Downloaded ${progress.successCount} of ${progress.successCount + progress.failureCount} items" }

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
    private fun fetchAlbums() {
        val familyIdValue = familyId ?: return

        viewModelScope.launch {
            getAlbumDisplayModelsUseCase(familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val possibleAlbums = result.data.filterNot { it.id == albumId }
                        _uiState.update { it.copy(albums = possibleAlbums, showOverflowMenuActionDialog = true) }

                        possibleAlbums.forEach { model ->
                            if (model.thumbnail == null) {
                                galleryRepository.fetchAlbumThumbnail(model.id)
                            }
                        }
                    }
                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }
    fun moveSelectedMediaToAlbum(newAlbumId: String) {
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
    fun deleteSelectedMedia() {
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
    // ----------------------------------- SELECT MODE
    fun toggleSelectionMode() {
        _uiState.update { it.copy(isSelectionModeActive = !it.isSelectionModeActive) }
        if (!uiState.value.isSelectionModeActive) {
            _uiState.update { it.copy(selectedMedia = emptySet()) }
        }
    }
    fun toggleAllMediaSelection() {
        when (_uiState.value.isAllMediaSelected) {
            true -> {
                _uiState.update { it.copy(
                    selectedMedia = emptySet(),
                    isAllMediaSelected = false,
                    isSelectionModeActive = false
                )}
            }
            false -> {
                _uiState.update { state ->
                    state.copy(
                        selectedMedia = _uiState.value.media.mapNotNull { it.id }.toSet(),
                        isAllMediaSelected = true
                    )
                }
            }
        }
    }
    fun toggleMediaSelection(mediaId: String?) {
        if (mediaId == null) return

        if (_uiState.value.selectedMedia.contains(mediaId)) {
            _uiState.update { state ->
                state.copy(selectedMedia = state.selectedMedia - mediaId)
            }
        }
        else {
            _uiState.update { state ->
                state.copy(selectedMedia = state.selectedMedia + mediaId)
            }
        }
    }
    // ---------------------------------------------------------------- OVERFLOW MENU + DIALOG
    fun toggleOverflowMenu(show: Boolean? = null) {
        _uiState.update { it.copy(showOverflowMenu = show ?: !it.showOverflowMenu) }
    }
    fun startOverflowAction(action: MenuAction) {
        _uiState.update {
            it.copy(
                overflowMenuAction = action,
                showOverflowMenu = false,
            )
        }
        if (action == MenuAction.SelectionActions.MOVE) {
            fetchAlbums()
        } else {
            _uiState.update { it.copy(showOverflowMenuActionDialog = true) }
        }
    }
    fun setActionDialogText(text: String) {
        _uiState.update { it.copy(actionDialogText = text) }
    }
    fun dismissOverflowMenuActionDialog() {
        _uiState.update {
            it.copy(
                showOverflowMenuActionDialog = false,
                overflowMenuAction = null,
                actionDialogText = "",
            )
        }
    }
    // ---------------------------------------------------------------- SHOW ERROR ALERT
    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(showAlertDialog = false, error = "") }
        }
    }
    fun showError(message: String) {
        _uiState.update { it.copy(showAlertDialog = true, error = message) }
    }
}
