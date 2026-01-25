package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.usecase.gallery.FetchAlbumMediaUseCase
import com.example.lifetogether.domain.usecase.image.DownloadMediaUseCase
import com.example.lifetogether.ui.model.MenuAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryMediaUiState(
    val mediaData: GalleryMedia? = null,
    val mediaList: List<GalleryMedia> = emptyList(),
    val currentIndex: Int = 0,
    val isDownloading: Boolean = false,
    val downloadMessage: String? = null,
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val isInitialized: Boolean = false,
    val showOverflowMenu: Boolean = false,
    val showOverflowMenuActionDialog: Boolean = false,
    val overflowMenuAction: MenuAction.GalleryMediaActions? = null,
    val actionDialogText: String = "",
)

@HiltViewModel
class GalleryMediaViewModel @Inject constructor(
    private val downloadMediaUseCase: DownloadMediaUseCase,
    private val fetchAlbumMediaUseCase: FetchAlbumMediaUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryMediaUiState())
    val uiState: StateFlow<GalleryMediaUiState> = _uiState.asStateFlow()

    private var familyId: String? = null
    private var albumId: String? = null
    private var initialIndex: Int = 0

    fun setUpMediaData(addedFamilyId: String, addedAlbumId: String, addedInitialIndex: Int) {
        familyId = addedFamilyId
        albumId = addedAlbumId
        initialIndex = addedInitialIndex
        _uiState.update { it.copy(isInitialized = true, currentIndex = addedInitialIndex) }
        loadAlbumMedia()
    }

    fun loadAlbumMedia() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        viewModelScope.launch {
            fetchAlbumMediaUseCase.invoke(familyIdValue, albumIdValue)
                .collect { result ->
                    when (result) {
                        is ListItemsResultListener.Success -> {
                            val mediaList = result.listItems
                            _uiState.update { 
                                it.copy(
                                    mediaList = mediaList,
                                    mediaData = mediaList.getOrNull(initialIndex)
                                ) 
                            }
                        }
                        is ListItemsResultListener.Failure -> {
                            showError(result.message)
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

    // ---------------------------------------------------------------- Overflow menu
    fun toggleOverflowMenu(show: Boolean? = null) {
        _uiState.update { it.copy(showOverflowMenu = show ?: !it.showOverflowMenu) }
    }

    fun startOverflowAction(action: MenuAction.GalleryMediaActions) {
        _uiState.update {
            it.copy(
                overflowMenuAction = action,
                showOverflowMenuActionDialog = true,
                showOverflowMenu = false,
            )
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
    private fun showError(message: String) {
        _uiState.update { it.copy(showAlertDialog = true, error = message) }
    }
}
