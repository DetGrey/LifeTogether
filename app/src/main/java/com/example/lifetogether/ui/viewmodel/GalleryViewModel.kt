package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.usecase.item.FetchListItemsUseCase
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val fetchListItemsUseCase: FetchListItemsUseCase,
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
            println("GalleryViewModel setting UID")
            familyId = addedFamilyId
            // Use the Family ID here (e.g., fetch list items)
            fetchAlbums()
            fetchGalleryImages()
            familyIdIsSet = true
        }
    }

    // ---------------------------------------------------------------- CONFIRMATION TYPES
    sealed class GalleryType {
        data object Albums : GalleryType()
        data class Images(val albumId: String, val albumName: String) : GalleryType()
    }

    private val _galleryType = MutableStateFlow<GalleryType>(GalleryType.Albums)
    val galleryType: StateFlow<GalleryType> = _galleryType.asStateFlow()

    fun toggleGalleryType(currentAlbumId: String? = null, currentAlbumName: String = "") {
        _galleryType.value = when (_galleryType.value) {
            GalleryType.Albums -> {
                if (currentAlbumId != null) {
                    val name = currentAlbumName.ifEmpty { "Gallery" }
                    GalleryType.Images(currentAlbumId, name)
                } else {
                    error = "Album does not exist"
                    showAlertDialog = true
                    GalleryType.Albums
                }
            }
            is GalleryType.Images -> GalleryType.Albums
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
                Album::class
            ).collect { result ->
                println("fetchListItemsUseCase result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        // Filter and map the result.listItems to only include Album instances
                        println("Items found: ${result.listItems}")
                        val albumItems = result.listItems.filterIsInstance<Album>()
                        if (albumItems.isNotEmpty()) {
                            println("_albums old value: ${_albums.value}")
                            _albums.value = albumItems
                            println("albums new value: ${albums.value}")

                        } else {
                            println("Error: No Album instances found in the result")
//                            error = "No Album instances found in the result"
//                            showAlertDialog = true
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

    // ---------------------------------------------------------------- GALLERY IMAGES
    private val _gallery = MutableStateFlow<List<GalleryImage>>(emptyList())
    val gallery: StateFlow<List<GalleryImage>> = _gallery.asStateFlow()

    val selectedAlbumImages: StateFlow<List<GalleryImage>> = combine(_gallery, galleryType) { allImages, type ->
        when (type) {
            is GalleryType.Images -> {
                allImages.filter { it.albumId == type.albumId }.sortedByDescending { it.dateCreated }
            }
            is GalleryType.Albums -> {
                emptyList()
            }
        }
    }.stateIn(
        scope = viewModelScope, // Or your appropriate CoroutineScope
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun fetchGalleryImages() {
        viewModelScope.launch {
            fetchListItemsUseCase(
                familyId!!,
                Constants.GALLERY_IMAGES_TABLE,
                GalleryImage::class
            ).collect { result ->
                println("fetchListItemsUseCase result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        // Filter and map the result.listItems to only include GalleryImage instances
                        println("Items found: ${result.listItems}")
                        val items = result.listItems.filterIsInstance<GalleryImage>()
                        if (items.isNotEmpty()) {
                            println("_albums old value: ${_gallery.value}")
                            _gallery.value = items
                            println("albums new value: ${gallery.value}")

                        } else {
                            println("Error: No GalleryImage instances found in the result")
//                            error = "No GalleryImage instances found in the result"
//                            showAlertDialog = true
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
}
