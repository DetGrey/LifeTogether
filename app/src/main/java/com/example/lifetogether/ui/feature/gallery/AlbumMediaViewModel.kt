package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.usecase.gallery.DeleteAlbumUseCase
import com.example.lifetogether.domain.usecase.gallery.FetchAlbumMediaUseCase
import com.example.lifetogether.domain.usecase.image.FetchAlbumMediaThumbnailUseCase
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumMediaUiState(
    val album: Album? = null,
    val media: List<GalleryMedia> = emptyList(),
    val thumbnails: Map<String, ByteArray> = emptyMap(),
    val isSyncing: Boolean = false,
    val showOverflowMenu: Boolean = false,
    val showOverflowMenuActionDialog: Boolean = false,
    val overflowMenuAction: AlbumMediaViewModel.OverflowMenuActions? = null,
    val actionDialogText: String = "",
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val isInitialized: Boolean = false,
    val isPartialLoad: Boolean = false, // True when some media failed to load
    val isRefreshing: Boolean = false, // User-triggered refresh
)

@HiltViewModel
class AlbumMediaViewModel @Inject constructor(
    private val fetchAlbumMediaUseCase: FetchAlbumMediaUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
    private val fetchAlbumMediaThumbnailUseCase: FetchAlbumMediaThumbnailUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumMediaUiState())
    val uiState: StateFlow<AlbumMediaUiState> = _uiState.asStateFlow()

    enum class OverflowMenuActions {
        RENAME_ALBUM,
        SELECT_MEDIA,
        DELETE_ALBUM,
    }

    private var familyId: String? = null
    private var albumId: String? = null

    private var syncRetryAttempts = 0
    private val maxSyncRetryAttempts = 3

    fun setUpAlbumMedia(addedFamilyId: String, addedAlbumId: String) {
        if (_uiState.value.isInitialized && albumId == addedAlbumId) return

        familyId = addedFamilyId
        albumId = addedAlbumId
        syncRetryAttempts = 0

        _uiState.update { it.copy(isInitialized = true, isSyncing = false) }
        fetchAlbum()
        fetchAlbumMedia()
    }

    private fun fetchAlbum() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        viewModelScope.launch {
            fetchItemByIdUseCase.invoke(
                familyIdValue,
                albumIdValue,
                Constants.ALBUMS_TABLE,
                Album::class,
            ).collect { result ->
                when (result) {
                    is ItemResultListener.Success -> {
                        val album = result.item as? Album
                        if (album != null) {
                            _uiState.update { state ->
                                state.copy(
                                    album = album,
                                    isSyncing = album.count > 0 && state.media.isEmpty(),
                                )
                            }
                        } else {
                            showError("Cannot find the album")
                        }
                    }

                    is ItemResultListener.Failure -> showError(result.message)
                }
            }
        }
    }

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
            when (val result = updateItemUseCase.invoke(updatedAlbum, Constants.ALBUMS_TABLE)) {
                is ResultListener.Success -> {
                    _uiState.update { it.copy(album = updatedAlbum) }
                    dismissOverflowMenuActionDialog()
                }
                is ResultListener.Failure -> showError(result.message)
            }
        }
    }

    fun deleteAlbum(onDeleteSuccess: () -> Unit) {
        val albumIdValue = _uiState.value.album?.id ?: return

        viewModelScope.launch {
            when (val result = deleteAlbumUseCase.invoke(albumIdValue, _uiState.value.media)) {
                is ResultListener.Success -> {
                    dismissOverflowMenuActionDialog()
                    onDeleteSuccess()
                }
                is ResultListener.Failure -> showError(result.message)
            }
        }
    }

    fun retryFetchAlbumMedia() {
        _uiState.update { it.copy(isRefreshing = true) }
        syncRetryAttempts = 0
        fetchAlbumMedia()
    }

    fun onRefreshComplete() {
        _uiState.update { it.copy(isRefreshing = false) }
    }

    private fun fetchAlbumMedia() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        viewModelScope.launch {
            val expectedCount = _uiState.value.album?.count ?: 0
            if (expectedCount > 0 && _uiState.value.media.isEmpty()) {
                _uiState.update { it.copy(isSyncing = true) }
            }

            fetchAlbumMediaUseCase.invoke(
                familyIdValue,
                albumIdValue,
            ).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success -> handleMediaSuccess(result.listItems)
                    is ListItemsResultListener.Failure -> handleMediaFailure(result.message)
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

    private fun handleMediaFailure(message: String) {
        _uiState.update { it.copy(isSyncing = false, isRefreshing = false) }
        showError(message)
    }

    fun fetchThumbnail(mediaId: String) {
        if (_uiState.value.thumbnails.containsKey(mediaId)) return

        viewModelScope.launch(Dispatchers.IO) {
            when (val result = fetchAlbumMediaThumbnailUseCase.invoke(mediaId)) {
                is ByteArrayResultListener.Success -> {
                    _uiState.update { state ->
                        state.copy(thumbnails = state.thumbnails + (mediaId to result.byteArray))
                    }
                }
                is ByteArrayResultListener.Failure -> {
                    // Ignore missing thumbnail
                }
            }
        }
    }

    fun toggleOverflowMenu(show: Boolean? = null) {
        _uiState.update { it.copy(showOverflowMenu = show ?: !it.showOverflowMenu) }
    }

    fun startOverflowAction(action: OverflowMenuActions) {
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
