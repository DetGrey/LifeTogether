package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.FamilyInformationResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.usecase.family.DeleteFamilyUseCase
import com.example.lifetogether.domain.usecase.family.FetchFamilyInformationUseCase
import com.example.lifetogether.domain.usecase.family.LeaveFamilyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val leaveFamilyUseCase: LeaveFamilyUseCase,
    private val deleteFamilyUseCase: DeleteFamilyUseCase,
    private val fetchFamilyInformationUseCase: FetchFamilyInformationUseCase,
) : ViewModel() {
    // ---------------------------------------------------------------- FAMILY INFORMATION
    // Create a StateFlow to hold family information
    private val _familyInformation = MutableStateFlow<FamilyInformation?>(null)
    val familyInformation: StateFlow<FamilyInformation?> = _familyInformation.asStateFlow()

    fun setUpFamilyInformation(addedFamilyId: String) {
        fetchFamilyInformation(addedFamilyId)
    }

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

    // ---------------------------------------------------------------- CONFIRMATION DIALOGS
    enum class FamilyConfirmationTypes {
        LEAVE_FAMILY,
        ADD_MEMBER,
        REMOVE_MEMBER,
        DELETE_FAMILY,
    }

    var confirmationDialogType: FamilyConfirmationTypes? by mutableStateOf(null)
    var showConfirmationDialog: Boolean by mutableStateOf(false)
    var memberToRemove: FamilyMember? by mutableStateOf(null)

    fun closeConfirmationDialog() {
        showConfirmationDialog = false
    }

    // ---------------------------------------------------------------- FUNCTIONS
    private fun fetchFamilyInformation(familyId: String) {
        println("FirebaseViewModel before calling fetchFamilyInformationUseCase")
        viewModelScope.launch {
            fetchFamilyInformationUseCase.invoke(familyId = familyId).collect { result ->
                println("FirebaseViewModel fetchFamilyInformationUseCase result: $result")
                when (result) {
                    is FamilyInformationResultListener.Success -> {
                        _familyInformation.value = result.familyInformation
                    }

                    is FamilyInformationResultListener.Failure -> {
                        _familyInformation.value = null
                    }
                }
            }
        }
    }

    fun leaveFamily(
        familyId: String,
        uid: String,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            when (val result = leaveFamilyUseCase.invoke(familyId, uid)) {
                is ResultListener.Success -> {
                    closeConfirmationDialog()
                    onComplete()
                }
                is ResultListener.Failure -> {
                    closeConfirmationDialog()
                    error = result.message
                    showAlertDialog = true
                    onComplete()
                }
            }
        }
    }

    fun deleteFamily(
        familyId: String,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            when (val result = deleteFamilyUseCase.invoke(familyId)) {
                is ResultListener.Success -> {
                    closeConfirmationDialog()
                    onComplete()
                }
                is ResultListener.Failure -> {
                    closeConfirmationDialog()
                    error = result.message
                    showAlertDialog = true
                    onComplete()
                }
            }
        }
    }
}
