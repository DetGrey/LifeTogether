package com.example.lifetogether.ui.feature.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.usecase.gallery.FetchAlbumMediaUseCase
import com.example.lifetogether.domain.usecase.image.FetchAlbumMediaThumbnailUseCase
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumMediaViewModel @Inject constructor(
    private val fetchAlbumMediaUseCase: FetchAlbumMediaUseCase,
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
    private val fetchAlbumMediaThumbnailUseCase: FetchAlbumMediaThumbnailUseCase,
) : ViewModel() {
    // ---------------------------------------------------------------- ERROR
    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")
    fun toggleAlertDialog() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    // ---------------------------------------------------------------- Family ID
    private var familyIdIsSet = false
    var familyId: String? = null

    // ---------------------------------------------------------------- Album
    var albumId: String? by mutableStateOf(null)

    private val _album = MutableStateFlow<Album?>(null)
    val album: StateFlow<Album?> = _album.asStateFlow()

    private fun fetchAlbum() {
        viewModelScope.launch {
            fetchItemByIdUseCase.invoke(
                familyId!!,
                albumId!!,
                Constants.ALBUMS_TABLE,
                Album::class,
            ).collect { result ->
                println("fetchItemByIdUseCase result: $result")
                when (result) {
                    is ItemResultListener.Success -> {
                        // Filter and map the result.listItems to only include GalleryImage instances
                        if (result.item is Album) {
                            _album.value = result.item
                        } else {
                            println("Error: Cannot find the album")
                            error = "Cannot find the album"
                            showAlertDialog = true
                        }
                    }

                    is ItemResultListener.Failure -> {
                        // Handle failure, e.g., show an error message
                        println("Error: ${result.message}")
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUpAlbumMedia(
        addedFamilyId: String,
        addedAlbumId: String,
    ) {
        if (!familyIdIsSet) {
            println("AlbumMediaViewModel setting familyId and albumId")
            familyId = addedFamilyId
            albumId = addedAlbumId
            // Use the Family ID here (e.g., fetch list items)
            fetchAlbum()
            fetchAlbumMedia()
            familyIdIsSet = true
        }
    }

    // ---------------------------------------------------------------- GALLERY IMAGES
    private val _albumMedia = MutableStateFlow<List<GalleryMedia>>(emptyList())
    val albumMedia: StateFlow<List<GalleryMedia>> = _albumMedia.asStateFlow()

    private fun fetchAlbumMedia() {
        viewModelScope.launch {
            fetchAlbumMediaUseCase.invoke(
                familyId!!,
                albumId!!,
            ).collect { result ->
                println("fetchAlbumMediaUseCase result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        // Filter and map the result.listItems to only include GalleryMedia instances
                        println("Items found: ${result.listItems}")
                        val items = result.listItems
                        if (items.isNotEmpty()) {
                            println("_albums old value: ${_albumMedia.value}")
                            val sortedMedia = items.sortedByDescending { it.dateCreated } // Sort by newest first
                            _albumMedia.value = sortedMedia // Emit sorted list

                            println("albums new value: ${albumMedia.value}")
                        } else {
                            println("Error: No GalleryMedia instances found in the result")
                        }
                    }

                    is ListItemsResultListener.Failure -> {
                        // Handle failure, e.g., show an error message
                        println("Error: ${result.message}")
                        error = result.message
                        showAlertDialog = true
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- THUMBNAILS
    private val _thumbnails = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val thumbnails: StateFlow<Map<String, ByteArray>> = _thumbnails.asStateFlow()

    fun fetchThumbnail(mediaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = fetchAlbumMediaThumbnailUseCase.invoke(mediaId)) {
                is ByteArrayResultListener.Success -> {
                    _thumbnails.value += mapOf(mediaId to result.byteArray)
                }
                is ByteArrayResultListener.Failure -> {
                }
            }
        }
    }
}
