package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.enums.SettingsConfirmationTypes
import com.example.lifetogether.domain.usecase.family.CreateNewFamilyUseCase
import com.example.lifetogether.domain.usecase.family.JoinFamilyUseCase
import com.example.lifetogether.domain.usecase.family.LeaveFamilyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val createNewFamilyUseCase: CreateNewFamilyUseCase,
    private val joinFamilyUseCase: JoinFamilyUseCase,
    private val leaveFamilyUseCase: LeaveFamilyUseCase,
) : ViewModel() {
    var confirmationDialogType: SettingsConfirmationTypes? by mutableStateOf(null)
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    var addedFamilyId: String by mutableStateOf("")

    fun closeConfirmationDialog() {
        showConfirmationDialog = false
        addedFamilyId = ""
    }

    fun joinFamily(uid: String) {
        if (addedFamilyId.isEmpty()) {
            return
        }

        viewModelScope.launch {
            when (joinFamilyUseCase.invoke(addedFamilyId, uid)) {
                is ResultListener.Success -> {
                    closeConfirmationDialog()
                }
                is ResultListener.Failure -> {
                    // TODO popup
                }
            }
        }
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
    fun leaveFamily(
        familyId: String,
        uid: String,
    ) {
        viewModelScope.launch {
            when (leaveFamilyUseCase.invoke(familyId, uid)) {
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
