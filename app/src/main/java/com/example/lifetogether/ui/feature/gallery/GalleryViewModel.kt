package com.example.lifetogether.ui.feature.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.usecase.image.FetchAlbumThumbnailUseCase
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
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

data class GalleryUiState(
    val albums: List<Album> = emptyList(),
    val thumbnails: Map<String, ByteArray> = emptyMap(),
    val showNewAlbumDialog: Boolean = false,
    val newAlbumName: String = "",
    val showAlertDialog: Boolean = false,
    val error: String = "",
    val isInitialized: Boolean = false,
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val fetchAlbumThumbnailUseCase: FetchAlbumThumbnailUseCase,
    private val saveItemUseCase: SaveItemUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var familyId: String? = null

    fun setUpGallery(addedFamilyId: String) {
        if (_uiState.value.isInitialized) return

        familyId = addedFamilyId
        fetchAlbums()
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
            when (val result = saveItemUseCase(album, Constants.ALBUMS_TABLE)) {
                is StringResultListener.Success -> closeNewAlbumDialog()
                is StringResultListener.Failure -> showError(result.message)
            }
        }
    }

    private fun fetchAlbums() {
        val familyIdValue = familyId ?: return

        viewModelScope.launch {
            fetchListItemsUseCase(
                familyIdValue,
                Constants.ALBUMS_TABLE,
                Album::class,
            ).collect { result ->
                when (result) {
                    is ListItemsResultListener.Success -> handleAlbumsSuccess(result.listItems.filterIsInstance<Album>())
                    is ListItemsResultListener.Failure -> showError(result.message)
                }
            }
        }
    }

    private fun handleAlbumsSuccess(albumItems: List<Album>) {
        if (albumItems.isEmpty()) {
            _uiState.update { it.copy(albums = emptyList(), isInitialized = true) }
            return
        }

        val sortedAlbums = albumItems.sortedBy { it.itemName }
        _uiState.update {
            it.copy(
                albums = sortedAlbums,
                isInitialized = true,
            )
        }
        fetchThumbnails(sortedAlbums)
    }

    private fun fetchThumbnails(albums: List<Album>) {
        albums.forEach { album ->
            val albumId = album.id ?: return@forEach
            val alreadyLoaded = _uiState.value.thumbnails.containsKey(albumId)
            if (alreadyLoaded) return@forEach

            viewModelScope.launch(Dispatchers.IO) {
                when (val result = fetchAlbumThumbnailUseCase.invoke(albumId)) {
                    is ByteArrayResultListener.Success -> {
                        _uiState.update { current ->
                            current.copy(thumbnails = current.thumbnails + (albumId to result.byteArray))
                        }
                    }
                    is ByteArrayResultListener.Failure -> {
                        // Ignore missing thumbnails
                    }
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
