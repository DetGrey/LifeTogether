package com.example.lifetogether.ui.feature.gallery

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ItemResultListener
import com.example.lifetogether.domain.model.gallery.GalleryMedia
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
class GalleryMediaViewModel @Inject constructor(
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
    var mediaId: String? by mutableStateOf(null)

    private val _mediaData = MutableStateFlow<GalleryMedia?>(null)
    val mediaData: StateFlow<GalleryMedia?> = _mediaData.asStateFlow()

    private fun fetchMediaData() {
        viewModelScope.launch {
            fetchItemByIdUseCase.invoke(
                familyId!!,
                mediaId!!,
                Constants.GALLERY_MEDIA_TABLE,
                GalleryMedia::class,
            ).collect { result ->
                println("fetchItemByIdUseCase result: $result")
                when (result) {
                    is ItemResultListener.Success -> {
                        // Filter and map the result.listItems to only include GalleryImage instances
                        if (result.item is GalleryMedia) {
                            _mediaData.value = result.item
                        } else {
                            println("Error: Cannot find the media")
                            error = "Cannot find the media"
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
    fun setUpMediaData(
        addedFamilyId: String,
        addedImageId: String,
    ) {
        if (!familyIdIsSet) {
            println("GalleryMediaViewModel setting familyId and mediaId")
            familyId = addedFamilyId
            mediaId = addedImageId
            // Use the Family ID here (e.g., fetch list items)
            fetchMediaData()
            familyIdIsSet = true
        }
    }
}
