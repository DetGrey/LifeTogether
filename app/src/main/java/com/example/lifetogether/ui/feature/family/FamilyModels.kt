package com.example.lifetogether.ui.feature.family

import android.net.Uri
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import java.util.Date

sealed interface FamilyUiState {
    data object Loading : FamilyUiState

    data class Content(
        val familyId: String?,
        val uid: String?,
        val familyInformation: FamilyInformation?,
        val togetherSinceDraft: Date? = null,
        val isTogetherSinceEditing: Boolean = false,
        val dialog: FamilyDialogState? = null,
    ) : FamilyUiState
}

sealed interface FamilyDialogState {
    data object DatePicker : FamilyDialogState
    data object LeaveFamily : FamilyDialogState
    data object AddMember : FamilyDialogState
    data class RemoveMember(val member: FamilyMember) : FamilyDialogState
    data object DeleteFamily : FamilyDialogState
}

sealed interface FamilyUiEvent {
    data class ImageSelected(val uri: Uri) : FamilyUiEvent
    data object TogetherSinceEditClicked : FamilyUiEvent
    data object TogetherSinceSaveClicked : FamilyUiEvent
    data object TogetherSinceClearClicked : FamilyUiEvent
    data class TogetherSinceDateSelected(val date: Date) : FamilyUiEvent
    data object AddMemberClicked : FamilyUiEvent
    data object LeaveFamilyClicked : FamilyUiEvent
    data object DeleteFamilyClicked : FamilyUiEvent
    data class RemoveMemberClicked(val member: FamilyMember) : FamilyUiEvent
    data object DismissDialog : FamilyUiEvent
    data object ConfirmDialog : FamilyUiEvent
}

sealed interface FamilyNavigationEvent {
    data object NavigateBack : FamilyNavigationEvent
}

sealed interface FamilyCommand {
    data object NavigateBack : FamilyCommand
    data class CopyFamilyId(val familyId: String) : FamilyCommand
}
