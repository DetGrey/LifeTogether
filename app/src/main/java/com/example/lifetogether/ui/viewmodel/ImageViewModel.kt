package com.example.lifetogether.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.converter.toBitmap
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.usecase.image.FetchImageByteArrayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageViewModel @Inject constructor(
    private val fetchImageByteArrayUseCase: FetchImageByteArrayUseCase,
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
            fetchImageByteArrayUseCase.invoke(imageType).collect { result ->
                println("fetchImageByteArrayUseCase result: $result")
                when (result) {
                    is ByteArrayResultListener.Success -> {
                        _bitmap.value = result.byteArray.toBitmap()
                    }

                    is ByteArrayResultListener.Failure -> {
                        _bitmap.value = null
                        println("Error: ${result.message}")
                        if (result.message != "No ByteArray found") {
                            onError(result.message)
                        }
                    }
                }
            }
        }
    }
}
