package com.example.lifetogether.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.converter.toBitmap
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.usecase.image.FetchImageByteArrayUseCase
import com.example.lifetogether.domain.usecase.user.ChangeNameUseCase
import com.example.lifetogether.domain.usecase.user.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val fetchImageByteArrayUseCase: FetchImageByteArrayUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val changeNameUseCase: ChangeNameUseCase,
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

    // ---------------------------------------------------------------- BITMAP
    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    fun setUpProfile(uid: String) {
        println("ProfileViewModel setUpProfile")
        viewModelScope.launch {
            fetchImageByteArrayUseCase.invoke(ImageType.ProfileImage(uid)).collect { result ->
                println("fetchImageByteArrayUseCase result: $result")
                when (result) {
                    is ByteArrayResultListener.Success -> {
                        _bitmap.value = result.byteArray.toBitmap()
                    }

                    is ByteArrayResultListener.Failure -> {
                        _bitmap.value = null
                        println("Error: ${result.message}")
                        if (result.message != "No ByteArray found") {
                            error = result.message
                            showAlertDialog = true
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- CONFIRMATION TYPES
    enum class ProfileConfirmationType {
        LOGOUT, NAME, PASSWORD
    }

    var confirmationDialogType: ProfileConfirmationType? by mutableStateOf(null)

    var showConfirmationDialog: Boolean by mutableStateOf(false)
    var showImageUploadDialog: Boolean by mutableStateOf(false)

    fun closeConfirmationDialog() {
        showConfirmationDialog = false
        newName = ""
    }

    // ---------------------------------------------------------------- LOGOUT
    fun logout(
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            val result = logoutUseCase.invoke()
            if (result is ResultListener.Success) {
                println("ProfileViewModel: Logout successful")
                onSuccess()
            } else if (result is ResultListener.Failure) {
                // TODO
            }
        }
    }

    // ---------------------------------------------------------------- CHANGE NAME
    var newName: String by mutableStateOf("")

    fun changeName(uid: String) {
        val name = newName
        if (name.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val result = changeNameUseCase.invoke(uid, name)
            if (result is ResultListener.Success) {
                closeConfirmationDialog()
            } else if (result is ResultListener.Failure) {
                // TODO
            }
        }
    }
}
