package com.example.lifetogether.ui.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.SettingsConfirmationTypes
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val familyRepository: FamilyRepository,
) : ViewModel() {
    private val _userInformation = MutableStateFlow<UserInformation?>(null)
    val userInformation: StateFlow<UserInformation?> = _userInformation.asStateFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                _userInformation.value = (state as? SessionState.Authenticated)?.user
            }
        }
    }

    var confirmationDialogType: SettingsConfirmationTypes? by mutableStateOf(null)
    var showConfirmationDialog: Boolean by mutableStateOf(false)

    var addedFamilyId: String by mutableStateOf("")

    fun closeConfirmationDialog() {
        showConfirmationDialog = false
        addedFamilyId = ""
    }

    fun joinFamily() {
        if (addedFamilyId.isEmpty()) return
        val uid = _userInformation.value?.uid ?: return
        val name = _userInformation.value?.name ?: return

        viewModelScope.launch {
            when (familyRepository.joinFamily(addedFamilyId, uid, name)) {
                is Result.Success -> {
                    closeConfirmationDialog()
                }
                is Result.Failure -> {
                    // TODO popup
                }
            }
        }
    }

    fun createNewFamily() {
        val uid = _userInformation.value?.uid ?: return
        val name = _userInformation.value?.name ?: return

        viewModelScope.launch {
            when (familyRepository.createNewFamily(uid, name)) {
                is Result.Success -> {
                    closeConfirmationDialog()
                }
                is Result.Failure -> {
                    // TODO popup
                }
            }
        }
    }
}
