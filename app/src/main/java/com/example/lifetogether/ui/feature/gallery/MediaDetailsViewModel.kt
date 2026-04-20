package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.usecase.gallery.DeleteMediaUseCase
import com.example.lifetogether.domain.usecase.image.DownloadMediaUseCase
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.ui.model.MenuAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaDetailsUiState(
    val mediaList: List<GalleryMedia> = emptyList(),
    val currentIndex: Int = 0,
    val isDownloading: Boolean = false,
    val downloadMessage: String? = null,
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val showOverflowMenu: Boolean = false,
    val showOverflowMenuActionDialog: Boolean = false,
    val overflowMenuAction: MenuAction.MediaDetailsActions? = null,
    val actionDialogText: String = "",
    var offsetY: Float = 0f,
)

@HiltViewModel
class MediaDetailsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val galleryRepository: GalleryRepository,
    private val downloadMediaUseCase: DownloadMediaUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MediaDetailsUiState())
    val uiState: StateFlow<MediaDetailsUiState> = _uiState.asStateFlow()

    private var familyId: String? = null
    private var albumId: String? = null
    private var initialIndex: Int = 0

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    albumId?.let { loadAlbumMedia() }
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                }
            }
        }
    }

    fun setUp(addedAlbumId: String, addedInitialIndex: Int) {
        albumId = addedAlbumId
        initialIndex = addedInitialIndex
        _uiState.update { it.copy(currentIndex = addedInitialIndex) }
        if (familyId != null) {
            loadAlbumMedia()
        }
    }

    fun loadAlbumMedia() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        viewModelScope.launch {
            galleryRepository.observeAlbumMedia(familyIdValue, albumIdValue)
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    mediaList = result.data,
                                )
                            }
                        }
                        is Result.Failure -> {
                            showError(result.error)
                        }
                    }
                }
        }
    }

    fun downloadMedia(index: Int? = null) {
        val mediaIndex = index ?: _uiState.value.currentIndex
        val currentMediaId = _uiState.value.mediaList.getOrNull(mediaIndex)?.id
        val familyIdValue = familyId

        if (currentMediaId == null || familyIdValue == null) {
            showError("Media data not available")
            return
        }

        viewModelScope.launch {
            downloadMediaUseCase(
                mediaIds = listOf(currentMediaId),
                familyId = familyIdValue,
            ).collect { progress ->
                when (progress) {
                    is SaveProgress.Loading -> {
                        _uiState.update { it.copy(isDownloading = true, downloadMessage = "Downloading ${progress.current} of ${progress.total}") }
                    }
                    is SaveProgress.Finished -> {
                        if (progress.failureCount == 0) {
                            dismissOverflowMenuActionDialog()
                            val message = if (progress.failureCount == 0) { "Downloaded 1 item" }
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

    fun deleteMedia(index: Int? = null) {
        val mediaIndex = index ?: _uiState.value.currentIndex
        val currentMedia = _uiState.value.mediaList.getOrNull(mediaIndex) ?: return

        viewModelScope.launch {
            when (val result = deleteMediaUseCase.invoke(currentMedia.albumId, listOf(currentMedia))) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                }
                is Result.Failure -> showError(result.error)
            }
        }
    }

    // ---------------------------------------------------------------- Overflow menu
    fun toggleOverflowMenu(show: Boolean? = null) {
        _uiState.update { it.copy(showOverflowMenu = show ?: !it.showOverflowMenu) }
    }

    fun startOverflowAction(action: MenuAction.MediaDetailsActions) {
        _uiState.update {
            it.copy(
                overflowMenuAction = action,
                showOverflowMenuActionDialog = true,
                showOverflowMenu = false,
            )
        }
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
    // ---------------------------------------------------------------- DRAG
    // TODO this was changed from 0.4f just to make it look nice but probably means some part is not showing
    private fun getMaxOffset(totalHeight: Int) = -totalHeight * 0.35f

    fun onVerticalDrag(dragAmount: Float, totalHeight: Int) {
        val maxOffset = getMaxOffset(totalHeight)
        val newOffset = (uiState.value.offsetY + dragAmount).coerceIn(maxOffset, 0f)
        _uiState.update { it.copy(offsetY = newOffset) }
    }

    fun onDragEnd(totalHeight: Int) {
        val currentOffset = uiState.value.offsetY
        val maxOffset = getMaxOffset(totalHeight)

        // SNAP LOGIC:
        // If the panel is more than 30% open, snap to fully open.
        // Otherwise, snap back to closed.
        val snapTarget = if (currentOffset < maxOffset * 0.3f) {
            maxOffset
        } else {
            0f
        }
        _uiState.update { it.copy(offsetY = snapTarget) }
    }
    // ---------------------------------------------------------------- SHOW ERROR ALERT
    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(showAlertDialog = false, error = "") }
        }
    }
    private fun showError(message: String) {
        _uiState.update { it.copy(showAlertDialog = true, error = message) }
    }
}
