package com.example.lifetogether.ui.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.converter.toBitmap
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageUploadViewModel @Inject constructor(
    private val uploadImageUseCase: UploadImageUseCase,
) : ViewModel() {
    var error: String by mutableStateOf("")

    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun setImageUri(uri: Uri, contentResolver: ContentResolver) {
        _imageUri.value = uri
        _bitmap.value = uri.toBitmap(contentResolver)
    }

    fun uploadPhoto(
        imageType: ImageType,
        context: Context,
    ) {
        val uri = imageUri.value ?: return
        _uploadState.value = UploadState.Uploading

        viewModelScope.launch {
            val result = uploadImageUseCase.invoke(uri, imageType, context)
            when (result) {
                is ResultListener.Success -> _uploadState.value = UploadState.Success
                is ResultListener.Failure -> _uploadState.value = UploadState.Failure(result.message)
            }
        }
    }
}
