package com.example.lifetogether.ui.feature.family

import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember

sealed interface FamilyUiState {
    data object Loading : FamilyUiState

    data class Content(
        val familyId: String?,
        val uid: String?,
        val familyInformation: FamilyInformation?,
        val showConfirmationDialog: Boolean = false,
        val confirmationDialogType: FamilyConfirmationType? = null,
        val memberToRemove: FamilyMember? = null,
        val showImageUploadDialog: Boolean = false,
    ) : FamilyUiState
}

enum class FamilyConfirmationType {
    LEAVE_FAMILY,
    ADD_MEMBER,
    REMOVE_MEMBER,
    DELETE_FAMILY,
}

sealed interface FamilyUiEvent {
    data object AddImageClicked : FamilyUiEvent
    data object ImageUploadDismissed : FamilyUiEvent
    data object ImageUploadConfirmed : FamilyUiEvent
    data object AddMemberClicked : FamilyUiEvent
    data object LeaveFamilyClicked : FamilyUiEvent
    data object DeleteFamilyClicked : FamilyUiEvent
    data class RemoveMemberClicked(val member: FamilyMember) : FamilyUiEvent
    data object DismissConfirmationDialog : FamilyUiEvent
    data object ConfirmConfirmationDialog : FamilyUiEvent
}

sealed interface FamilyNavigationEvent {
    data object NavigateBack : FamilyNavigationEvent
}

sealed interface FamilyCommand {
    data object NavigateBack : FamilyCommand
    data class CopyFamilyId(val familyId: String) : FamilyCommand
}
