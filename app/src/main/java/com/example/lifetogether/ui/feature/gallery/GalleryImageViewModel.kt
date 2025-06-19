package com.example.lifetogether.ui.feature.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryImageViewModel @Inject constructor(
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
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
    var imageId: String? by mutableStateOf(null)

    private val _imageData = MutableStateFlow<GalleryImage?>(null)
    val imageData: StateFlow<GalleryImage?> = _imageData.asStateFlow()

    private fun fetchImageData() {
        viewModelScope.launch {
            fetchItemByIdUseCase.invoke(
                familyId!!,
                imageId!!,
                Constants.GALLERY_IMAGES_TABLE,
                GalleryImage::class,
            ).collect { result ->
                println("fetchItemByIdUseCase result: $result")
                when (result) {
                    is ItemResultListener.Success -> {
                        // Filter and map the result.listItems to only include GalleryImage instances
                        if (result.item is GalleryImage) {
                            _imageData.value = result.item
                        } else {
                            println("Error: Cannot find the image")
                            error = "Cannot find the image"
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
    fun setUpImageData(
        addedFamilyId: String,
        addedImageId: String,
    ) {
        if (!familyIdIsSet) {
            println("GalleryImageViewModel setting familyId and imageId")
            familyId = addedFamilyId
            imageId = addedImageId
            // Use the Family ID here (e.g., fetch list items)
            fetchImageData()
            familyIdIsSet = true
        }
    }
}
