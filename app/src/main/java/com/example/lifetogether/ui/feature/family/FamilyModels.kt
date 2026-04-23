package com.example.lifetogether.ui.feature.family

import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember

data class FamilyUiState(
    val familyId: String? = null,
    val uid: String? = null,
    val familyInformation: FamilyInformation? = null,
    val showConfirmationDialog: Boolean = false,
    val confirmationDialogType: FamilyConfirmationType? = null,
    val memberToRemove: FamilyMember? = null,
)

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
