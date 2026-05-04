package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.gallery.DeleteMediaUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity
import com.example.lifetogether.ui.model.MenuAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
class MediaDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val galleryRepository: GalleryRepository,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaDetailsUiState())
    val uiState: StateFlow<MediaDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private var familyId: String? = null
    private var loadAlbumMediaJob: Job? = null
    private val albumId: String? = savedStateHandle["albumId"]
    private val initialIndex: Int = savedStateHandle["initialIndex"] ?: 0

    init {
        _uiState.update { it.copy(currentIndex = initialIndex) }
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    observeAlbumMedia()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    loadAlbumMediaJob?.cancel()
                    loadAlbumMediaJob = null
                }
            }
        }
    }

    fun onEvent(event: MediaDetailsUiEvent) {
        when (event) {
            is MediaDetailsUiEvent.VerticalDrag -> onVerticalDrag(event.dragAmount, event.totalHeight)
            is MediaDetailsUiEvent.DragEnd -> onDragEnd(event.totalHeight)
            MediaDetailsUiEvent.ToggleOverflowMenu -> toggleOverflowMenu()
            is MediaDetailsUiEvent.StartOverflowAction -> startOverflowAction(event.action)
            MediaDetailsUiEvent.DismissOverflowMenuActionDialog -> dismissOverflowMenuActionDialog()
            is MediaDetailsUiEvent.DownloadMedia -> downloadMedia(event.index)
            is MediaDetailsUiEvent.DeleteMedia -> deleteMedia(event.index)
        }
    }

    private fun observeAlbumMedia() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        loadAlbumMediaJob?.cancel()
        loadAlbumMediaJob = viewModelScope.launch {
            galleryRepository.observeAlbumMedia(familyIdValue, albumIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                mediaList = result.data,
                            )
                        }
                    }

                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun downloadMedia(index: Int? = null) {
        val mediaIndex = index ?: _uiState.value.currentIndex
        val currentMediaId = _uiState.value.mediaList.getOrNull(mediaIndex)?.id
        val familyIdValue = familyId

        if (currentMediaId == null || familyIdValue == null) {
            showError("Media data not available")
            return
        }

        viewModelScope.launch {
            galleryRepository.downloadMediaToGallery(
                mediaIds = listOf(currentMediaId),
                familyId = familyIdValue,
            ).collect { progress ->
                when (progress) {
                    is SaveProgress.Loading -> {
                        showProgress(
                            title = "Downloading...",
                            message = "Downloading ${progress.current} of ${progress.total}",
                        )
                    }

                    is SaveProgress.Finished -> {
                        if (progress.failureCount == 0) {
                            dismissOverflowMenuActionDialog()
                            showProgress(
                                title = "Download complete",
                                message = "Downloaded 1 item",
                                showProgress = false,
                            )
                            delay(2000)
                            hideProgress()
                        } else {
                            hideProgress()
                            showError("Failed to download media")
                        }
                    }

                    is SaveProgress.Error -> {
                        hideProgress()
                        showError(progress.message)
                    }
                }
            }
        }
    }

    private fun deleteMedia(index: Int? = null) {
        val mediaIndex = index ?: _uiState.value.currentIndex
        val currentMedia = _uiState.value.mediaList.getOrNull(mediaIndex) ?: return

        viewModelScope.launch {
            when (val result = deleteMediaUseCase.invoke(currentMedia.albumId, listOf(currentMedia))) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun toggleOverflowMenu(show: Boolean? = null) {
        _uiState.update { it.copy(showOverflowMenu = show ?: !it.showOverflowMenu) }
    }

    private fun startOverflowAction(action: MenuAction.MediaDetailsActions) {
        _uiState.update {
            it.copy(
                overflowMenuAction = action,
                showOverflowMenuActionDialog = true,
                showOverflowMenu = false,
            )
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

    // ---------------------------------------------------------------- DRAG
    // TODO this was changed from 0.4f just to make it look nice but probably means some part is not showing
    private fun getMaxOffset(totalHeight: Int) = -totalHeight * 0.35f

    private fun onVerticalDrag(dragAmount: Float, totalHeight: Int) {
        val maxOffset = getMaxOffset(totalHeight)
        val newOffset = (_uiState.value.offsetY + dragAmount).coerceIn(maxOffset, 0f)
        _uiState.update { it.copy(offsetY = newOffset) }
    }

    private fun onDragEnd(totalHeight: Int) {
        val currentOffset = uiState.value.offsetY
        val maxOffset = getMaxOffset(totalHeight)

        // Snap open only if the panel is dragged far enough up.
        // This makes a normal downward swipe close the panel more reliably.
        val snapTarget = if (currentOffset <= maxOffset * 0.5f) {
            maxOffset
        } else {
            0f
        }
        _uiState.update { it.copy(offsetY = snapTarget) }
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

    private fun showProgress(
        title: String,
        message: String,
        showProgress: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowProgressSnackbar(
                    title = title,
                    message = message,
                    severity = SnackbarSeverity.Info,
                    showProgress = showProgress,
                ),
            )
        }
    }

    private fun hideProgress() {
        viewModelScope.launch {
            _uiCommands.send(UiCommand.HideProgressSnackbar)
        }
    }
}
