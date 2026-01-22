package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.usecase.image.DownloadMediaUseCase
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.util.Constants
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
)

@HiltViewModel
class GalleryMediaViewModel @Inject constructor(
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
    private val downloadMediaUseCase: DownloadMediaUseCase,
    private val fetchAlbumMediaUseCase: com.example.lifetogether.domain.usecase.gallery.FetchAlbumMediaUseCase,
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
        val currentMedia = _uiState.value.mediaList.getOrNull(mediaIndex)
        val familyIdValue = familyId

        if (currentMedia == null || familyIdValue == null) {
            showError("Media data not available")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, downloadMessage = "Downloading...") }

            val result = downloadMediaUseCase.invoke(
                mediaId = currentMedia.id!!,
                familyId = familyIdValue,
                fileName = currentMedia.itemName,
            )

            when (result) {
                is ResultListener.Success -> {
                    _uiState.update { it.copy(downloadMessage = "Downloaded successfully!") }
                    delay(2000)
                    _uiState.update { it.copy(downloadMessage = null) }
                }
                is ResultListener.Failure -> {
                    _uiState.update { it.copy(downloadMessage = null) }
                    showError(result.message)
                }
            }
            _uiState.update { it.copy(isDownloading = false) }
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
