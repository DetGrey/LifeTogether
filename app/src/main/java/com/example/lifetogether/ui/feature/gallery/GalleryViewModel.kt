package com.example.lifetogether.ui.feature.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
    private val fetchAlbumThumbnailUseCase: FetchAlbumThumbnailUseCase,
    private val saveItemUseCase: SaveItemUseCase,
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

    // ---------------------------------------------------------------- SETUP/FETCH LIST
    fun setUpGallery(addedFamilyId: String) {
        if (!familyIdIsSet) {
            println("GalleryViewModel setting familyId")
            familyId = addedFamilyId
            // Use the Family ID here (e.g., fetch list items)
            fetchAlbums()
            familyIdIsSet = true
        }
    }

    // ---------------------------------------------------------------- ALBUMS
    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    var showNewAlbumDialog: Boolean by mutableStateOf(false)
    var newAlbumName: String by mutableStateOf("")

    fun closeNewAlbumDialog() {
        showNewAlbumDialog = false
        newAlbumName = ""
    }

    fun createNewAlbum() {
        if (newAlbumName.isEmpty()) {
            error = "Please enter an album name first"
            showAlertDialog = true
        }

        if (familyId == null) {
            error = "You are not logged in"
            showAlertDialog = true
        }

        val album = Album(
            itemName = newAlbumName,
            familyId = familyId!!,
        )

        viewModelScope.launch {
            val result: StringResultListener = saveItemUseCase(album, Constants.ALBUMS_TABLE)

            if (result is StringResultListener.Success) {
                closeNewAlbumDialog()
            } else if (result is StringResultListener.Failure) {
                println("Error: ${result.message}")
                error = result.message
                showAlertDialog = true
            }
        }
    }

    private fun fetchAlbums() {
        viewModelScope.launch {
            fetchListItemsUseCase(
                familyId!!,
                Constants.ALBUMS_TABLE,
                Album::class,
            ).collect { result ->
                println("fetchListItemsUseCase result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        // Filter and map the result.listItems to only include Album instances
                        println("Items found: ${result.listItems}")
                        val albumItems = result.listItems.filterIsInstance<Album>()
                        if (albumItems.isNotEmpty()) {
                            println("_albums old value: ${_albums.value}")
                            _albums.value = albumItems.sortedBy { it.itemName }
                            println("albums new value: ${albums.value}")
                            fetchThumbnails()
                        } else {
                            println("Error: No Album instances found in the result")
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

    // ---------------------------------------------------------------- ALBUM THUMBNAIL
    private val _thumbnails = MutableStateFlow<Map<String, ByteArray>>(emptyMap())
    val thumbnails: StateFlow<Map<String, ByteArray>> = _thumbnails.asStateFlow()

    fun fetchThumbnails() {
        for (album in albums.value) {
            if (album.id != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    when (val result = fetchAlbumThumbnailUseCase.invoke(album.id!!)) {
                        is ByteArrayResultListener.Success -> {
                            _thumbnails.value += mapOf(album.id!! to result.byteArray)
                        }
                        is ByteArrayResultListener.Failure -> {
                        }
                    }
                }
            }
        }
    }
}
