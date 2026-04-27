package com.example.lifetogether.ui.feature.family

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.feature.profile.ProfileDetails
import com.example.lifetogether.ui.feature.settings.SettingsItem
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun FamilyScreen(
    uiState: FamilyUiState,
    bitmap: Bitmap?,
    showImageUploadDialog: Boolean,
    onImageUpload: suspend (Uri) -> Result<Unit, AppError>,
    onUiEvent: (FamilyUiEvent) -> Unit,
    onNavigationEvent: (FamilyNavigationEvent) -> Unit,
) {
    val familyInformation = uiState.familyInformation
    val familyId = uiState.familyId
    val uid = uiState.uid
    val members = familyInformation?.members.orEmpty()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.padding(LifeTogetherTokens.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_back_arrow,
                        description = "back arrow icon",
                    ),
                    onLeftClick = {
                        onNavigationEvent(FamilyNavigationEvent.NavigateBack)
                    },
                    text = "Family",
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = LifeTogetherTokens.spacing.medium)
                                    .height(200.dp)
                                    .clip(shape = MaterialTheme.shapes.large)
                                    .background(color = MaterialTheme.colorScheme.onBackground),
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        modifier = Modifier.fillMaxSize(),
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "family image",
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier.size(LifeTogetherTokens.sizing.touchTargetMinimum),
                                contentAlignment = Alignment.Center,
                            ) {
                                AddButton(onClick = { onUiEvent(FamilyUiEvent.AddImageClicked) })
                            }
                        }
                    }

                    TextHeadingMedium(text = "Family members")

                    members.forEach { member ->
                        ProfileDetails(
                            icon = Icon(
                                resId = R.drawable.ic_profile,
                                description = "person icon",
                            ),
                            title = member.name ?: "Unknown Member",
                            value = if (member.uid == uid) "(Myself)" else "Remove",
                            onClick = if (member.uid == uid) {
                                null
                            } else {
                                { onUiEvent(FamilyUiEvent.RemoveMemberClicked(member)) }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))

                    TextHeadingMedium(text = "Settings")

                    SettingsItem(
                        icon = Icon(R.drawable.ic_profile, "profile icon"),
                        title = "Add family member",
                        link = "Share family ID",
                        linkClickable = {
                            onUiEvent(FamilyUiEvent.AddMemberClicked)
                        },
                    )

                    SettingsItem(
                        icon = Icon(R.drawable.ic_logout, "logout icon"),
                        title = "Leave family",
                        link = "Leave",
                        linkClickable = {
                            onUiEvent(FamilyUiEvent.LeaveFamilyClicked)
                        },
                    )

                    SettingsItem(
                        icon = Icon(R.drawable.ic_logout, "logout icon"),
                        title = "Delete family",
                        link = "Delete",
                        linkClickable = {
                            onUiEvent(FamilyUiEvent.DeleteFamilyClicked)
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.xLarge))
            }
        }

        if (uiState.showConfirmationDialog) {
            when (uiState.confirmationDialogType) {
                FamilyConfirmationType.LEAVE_FAMILY -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissConfirmationDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmConfirmationDialog) },
                    dialogTitle = "Leave family",
                    dialogMessage = "Are you sure you want to leave the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Leave",
                )

                FamilyConfirmationType.ADD_MEMBER -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissConfirmationDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmConfirmationDialog) },
                    dialogTitle = "Share family ID",
                    dialogMessage = "Family ID: $familyId",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Copy",
                )

                FamilyConfirmationType.REMOVE_MEMBER -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissConfirmationDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmConfirmationDialog) },
                    dialogTitle = "Remove member",
                    dialogMessage = "Are you sure you want to remove ${uiState.memberToRemove?.name} from the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Remove",
                )

                FamilyConfirmationType.DELETE_FAMILY -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissConfirmationDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmConfirmationDialog) },
                    dialogTitle = "Delete family",
                    dialogMessage = "Are you sure you want to delete the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete",
                )

                null -> Unit
            }
        }

        if (showImageUploadDialog && familyId != null) {
            ImageUploadDialog(
                onDismiss = { onUiEvent(FamilyUiEvent.ImageUploadDismissed) },
                onConfirm = { onUiEvent(FamilyUiEvent.ImageUploadConfirmed) },
                onUpload = onImageUpload,
                dialogTitle = "Upload family image",
                dialogMessage = "Select your new family image",
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload image",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FamilyScreenPreview() {
    LifeTogetherTheme {
        FamilyScreen(
            uiState = FamilyUiState(
                familyId = "family-123",
                uid = "uid-1",
                familyInformation = FamilyInformation(
                    familyId = "family-123",
                    members = emptyList(),
                ),
            ),
            bitmap = null,
            showImageUploadDialog = false,
            onImageUpload = { Result.Success(Unit) },
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
