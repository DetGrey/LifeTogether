package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.usecase.CreateNewFamilyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val createNewFamilyUseCase: CreateNewFamilyUseCase,
) : ViewModel() {
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    fun closeConfirmationDialog() {
        showConfirmationDialog = false
    }

    fun createNewFamily(uid: String) {
        viewModelScope.launch {
            when (createNewFamilyUseCase.invoke(uid)) {
                is ResultListener.Success -> {
                    closeConfirmationDialog()
                }
                is ResultListener.Failure -> {
                    // TODO popup
                }
            }
        }
    }
}
