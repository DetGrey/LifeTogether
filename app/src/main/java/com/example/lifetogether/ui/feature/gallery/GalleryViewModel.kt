package com.example.lifetogether.ui.feature.gallery

import com.example.lifetogether.domain.result.toUserMessage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.usecase.gallery.GetAlbumDisplayModelsUseCase
import com.example.lifetogether.ui.model.AlbumUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GalleryUiState(
    val albums: List<AlbumUiModel> = emptyList(),
    val showNewAlbumDialog: Boolean = false,
    val newAlbumName: String = "",
    val showAlertDialog: Boolean = false,
    val error: String = "",
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val getAlbumDisplayModelsUseCase: GetAlbumDisplayModelsUseCase,
    private val galleryRepository: GalleryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val requestedThumbnails = mutableSetOf<String>()
    private var familyId: String? = null

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    fetchAlbums()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                }
            }
        }
    }

    fun openNewAlbumDialog() {
        _uiState.update { it.copy(showNewAlbumDialog = true) }
    }

    fun closeNewAlbumDialog() {
        _uiState.update { it.copy(showNewAlbumDialog = false, newAlbumName = "") }
    }

    fun setNewAlbumName(name: String) {
        _uiState.update { it.copy(newAlbumName = name) }
    }

    fun createNewAlbum() {
        val familyIdValue = familyId
        val albumName = _uiState.value.newAlbumName.trim()

        if (albumName.isEmpty()) {
            showError("Please enter an album name first")
            return
        }

        if (familyIdValue == null) {
            showError("You are not logged in")
            return
        }

        val album = Album(
            itemName = albumName,
            familyId = familyIdValue,
        )

        viewModelScope.launch {
            when (val result = galleryRepository.saveAlbum(album)) {
                is Result.Success -> closeNewAlbumDialog()
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun fetchAlbums() {
        val familyIdValue = familyId ?: return

        viewModelScope.launch {
            getAlbumDisplayModelsUseCase.invoke(familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(albums = result.data) }

                        result.data.forEach { album ->
                            if (album.thumbnail == null && !requestedThumbnails.contains(album.id)) {
                                requestedThumbnails.add(album.id)
                                galleryRepository.fetchAlbumThumbnail(album.id)
                            }
                        }
                    }
                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
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
