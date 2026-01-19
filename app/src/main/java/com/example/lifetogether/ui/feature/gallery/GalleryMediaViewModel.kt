package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ItemResultListener
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryMediaUiState())
    val uiState: StateFlow<GalleryMediaUiState> = _uiState.asStateFlow()

    private var familyId: String? = null
    private var mediaId: String? = null

    fun setUpMediaData(addedFamilyId: String, addedImageId: String) {
        if (_uiState.value.isInitialized && mediaId == addedImageId) return

        familyId = addedFamilyId
        mediaId = addedImageId
        _uiState.update { it.copy(isInitialized = true) }
        fetchMediaData()
    }

    private fun fetchMediaData() {
        val familyIdValue = familyId ?: return
        val mediaIdValue = mediaId ?: return

        viewModelScope.launch {
            fetchItemByIdUseCase.invoke(
                familyIdValue,
                mediaIdValue,
                Constants.GALLERY_MEDIA_TABLE,
                GalleryMedia::class,
            ).collect { result ->
                when (result) {
                    is ItemResultListener.Success -> {
                        val media = result.item as? GalleryMedia
                        if (media != null) {
                            _uiState.update { it.copy(mediaData = media) }
                        } else {
                            showError("Cannot find the media")
                        }
                    }

                    is ItemResultListener.Failure -> showError(result.message)
                }
            }
        }
    }

    fun downloadMedia() {
        val currentMedia = _uiState.value.mediaData
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
