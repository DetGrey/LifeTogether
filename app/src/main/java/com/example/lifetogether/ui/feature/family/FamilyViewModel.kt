package com.example.lifetogether.ui.feature.family

import com.example.lifetogether.domain.result.toUserMessage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FamilyViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val familyRepository: FamilyRepository,
) : ViewModel() {
    // ---------------------------------------------------------------- SESSION
    private val _familyId = MutableStateFlow<String?>(null)
    val familyId: StateFlow<String?> = _familyId.asStateFlow()

    private val _uid = MutableStateFlow<String?>(null)
    val uid: StateFlow<String?> = _uid.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                val newFamilyId = authenticated?.user?.familyId
                if (newFamilyId != null && newFamilyId != _familyId.value) {
                    _familyId.value = newFamilyId
                    fetchFamilyInformation(newFamilyId)
                } else if (state is SessionState.Unauthenticated) {
                    _familyId.value = null
                    _uid.value = null
                }
                _uid.value = authenticated?.user?.uid
            }
        }
    }

    // ---------------------------------------------------------------- FAMILY INFORMATION
    // Create a StateFlow to hold family information
    private val _familyInformation = MutableStateFlow<FamilyInformation?>(null)
    val familyInformation: StateFlow<FamilyInformation?> = _familyInformation.asStateFlow()

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
        viewModelScope.launch {
            familyRepository.observeFamilyInformation(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        _familyInformation.value = result.data
                    }

                    is Result.Failure -> {
                        _familyInformation.value = null
                    }
                }
            }
        }
    }

    fun leaveFamily(
        memberUid: String? = null,
        onComplete: () -> Unit,
    ) {
        val familyIdValue = familyId.value ?: return
        val memberUid = memberUid ?: uid.value ?: return
        viewModelScope.launch {
            when (val result = familyRepository.leaveFamily(familyIdValue, memberUid)) {
                is Result.Success -> {
                    closeConfirmationDialog()
                    onComplete()
                }
                is Result.Failure -> {
                    closeConfirmationDialog()
                    error = result.error.toUserMessage()
                    showAlertDialog = true
                    onComplete()
                }
            }
        }
    }

    fun deleteFamily(
        onComplete: () -> Unit,
    ) {
        val familyIdValue = _familyId.value ?: return
        viewModelScope.launch {
            when (val result = familyRepository.deleteFamily(familyIdValue)) {
                is Result.Success -> {
                    closeConfirmationDialog()
                    onComplete()
                }
                is Result.Failure -> {
                    closeConfirmationDialog()
                    error = result.error.toUserMessage()
                    showAlertDialog = true
                    onComplete()
                }
            }
        }
    }
}
