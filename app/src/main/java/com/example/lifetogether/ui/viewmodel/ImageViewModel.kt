package com.example.lifetogether.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
) : ViewModel() {
    // ---------------------------------------------------------------- Upload Dialog
    var showImageUploadDialog: Boolean by mutableStateOf(false)

    // ---------------------------------------------------------------- BITMAP
    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    // ---------------------------------------------------------------- SET UP
    fun collectImageFlow(
        imageType: ImageType,
        onError: (String) -> Unit,
    ) {
        println("ImageViewModel collectImageFlow")
        viewModelScope.launch {
            imageRepository.getImageByteArray(imageType).collect { result ->
                println("getImageByteArray result: $result")
                when (result) {
                    is Result.Success -> {
                        _bitmap.value = result.data.toBitmap()
                    }

                    is Result.Failure -> {
                        _bitmap.value = null
                        println("Error: ${result.error}")
                        if (result.error != "No ByteArray found") {
                            onError(result.error)
                        }
                    }
                }
            }
        }
    }
}
