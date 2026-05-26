package com.example.lifetogether.ui.feature.family

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.AppIcon as AppIcon
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.DatePickerDialog
import com.example.lifetogether.ui.common.image.AnimatedBitmapImage
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.feature.profile.ProfileDetails
import com.example.lifetogether.ui.feature.settings.SettingsItem
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun FamilyScreen(
    uiState: FamilyUiState,
    bitmap: Bitmap?,
    onUiEvent: (FamilyUiEvent) -> Unit,
    onNavigationEvent: (FamilyNavigationEvent) -> Unit,
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { onUiEvent(FamilyUiEvent.ImageSelected(it)) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(FamilyNavigationEvent.NavigateBack)
                },
                text = "Family",
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is FamilyUiState.Loading,
            label = "family_loading",
            loadingContent = {
                Skeletons.SectionDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = uiState as? FamilyUiState.Content ?: return@AnimatedLoadingContent
            val familyInformation = content.familyInformation
            val familyId = content.familyId
            val uid = content.uid
            val members = familyInformation?.members.orEmpty()
            val togetherSince = if (content.isTogetherSinceEditing) {
                content.togetherSinceDraft
            } else {
                familyInformation?.togetherSince
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(LifeTogetherTokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
            ) {
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
                                        .background(color = MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    AnimatedBitmapImage(
                                        bitmap = bitmap,
                                        modifier = Modifier.fillMaxSize(),
                                        contentDescription = "family image",
                                    )
                                }

                                Box(
                                    modifier = Modifier.size(LifeTogetherTokens.sizing.touchTargetMinimum),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AddButton(onClick = { imagePickerLauncher.launch("image/*") })
                                }
                            }
                        }

                        FamilyTogetherSinceRow(
                            togetherSince = togetherSince,
                            isEditing = content.isTogetherSinceEditing,
                            showDatePicker = content.dialog is FamilyDialogState.DatePicker,
                            onEditClick = { onUiEvent(FamilyUiEvent.TogetherSinceEditClicked) },
                            onSaveClick = { onUiEvent(FamilyUiEvent.TogetherSinceSaveClicked) },
                            onClearClick = { onUiEvent(FamilyUiEvent.TogetherSinceClearClicked) },
                            onDateSelected = { onUiEvent(FamilyUiEvent.TogetherSinceDateSelected(it)) },
                            onDatePickerDismissed = { onUiEvent(FamilyUiEvent.DismissDialog) },
                        )

                        TextHeadingMedium(text = "Family members")

                        members.forEach { member ->
                            ProfileDetails(
                                appIcon = AppIcon(
                                    resId = R.drawable.ic_profile,
                                    description = "person icon",
                                ),
                                title = member.name,
                                value = if (member.uid == uid) "(Myself)" else "Remove",
                                onClick = if (member.uid == uid) {
                                    null
                                } else {
                                    { onUiEvent(FamilyUiEvent.RemoveMemberClicked(member)) }
                                },
                                member = member,
                            )
                        }

                        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))

                        TextHeadingMedium(text = "Settings")

                        SettingsItem(
                            appIcon = AppIcon(R.drawable.ic_profile, "profile icon"),
                            title = "Add family member",
                            link = "Share family ID",
                            isLinkClickable = true,
                            onClick = {
                                onUiEvent(FamilyUiEvent.AddMemberClicked)
                            },
                        )

                        SettingsItem(
                            appIcon = AppIcon(R.drawable.ic_logout, "logout icon"),
                            title = "Leave family",
                            link = "Leave",
                            isLinkClickable = true,
                            onClick = {
                                onUiEvent(FamilyUiEvent.LeaveFamilyClicked)
                            },
                        )

                        SettingsItem(
                            appIcon = AppIcon(R.drawable.ic_delete, "logout icon"),
                            title = "Delete family",
                            link = "Delete",
                            isLinkClickable = true,
                            onClick = {
                                onUiEvent(FamilyUiEvent.DeleteFamilyClicked)
                            },
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.xLarge))
                }
            }

            when (val dialog = content.dialog) {
                is FamilyDialogState.LeaveFamily -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmDialog) },
                    dialogTitle = "Leave family",
                    dialogMessage = "Are you sure you want to leave the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Leave",
                )

                is FamilyDialogState.AddMember -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmDialog) },
                    dialogTitle = "Share family ID",
                    dialogMessage = "Family ID: $familyId",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Copy",
                )

                is FamilyDialogState.RemoveMember -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmDialog) },
                    dialogTitle = "Remove member",
                    dialogMessage = "Are you sure you want to remove ${dialog.member.name} from the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Remove",
                )

                is FamilyDialogState.DeleteFamily -> ConfirmationDialog(
                    onDismiss = { onUiEvent(FamilyUiEvent.DismissDialog) },
                    onConfirm = { onUiEvent(FamilyUiEvent.ConfirmDialog) },
                    dialogTitle = "Delete family",
                    dialogMessage = "Are you sure you want to delete the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete",
                )

                is FamilyDialogState.DatePicker, null -> Unit
            }

        }
    }
}

@Composable
private fun FamilyTogetherSinceRow(
    togetherSince: Date?,
    isEditing: Boolean,
    showDatePicker: Boolean,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onClearClick: () -> Unit,
    onDateSelected: (Date) -> Unit,
    onDatePickerDismissed: () -> Unit,
) {
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = togetherSince,
            onDismiss = onDatePickerDismissed,
            onDateSelected = onDateSelected,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isEditing) {
                    Modifier.clickable { onEditClick() }
                } else {
                    Modifier
                },
            ),
    ) {
        val endPadding = if (isEditing) 0.dp else LifeTogetherTokens.spacing.small
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = LifeTogetherTokens.spacing.small)
                .padding(
                    start = LifeTogetherTokens.spacing.medium,
                    end = endPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextLabel(
                text = "Together since:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.small))

            TextDefault(
                text = togetherSince?.toFullDateString() ?: "Add date >",
                color = if (togetherSince == null) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier.weight(1f),
            )

            if (isEditing) {
                if (togetherSince != null) {
                    IconButton(onClick = onClearClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "edit family milestone date",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onSaveClick) {
                    TextDefault(
                        text = "Save",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "edit family milestone date",
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FamilyScreenPreview() {
    LifeTogetherTheme {
        FamilyScreen(
            uiState = FamilyUiState.Content(
                familyId = "family-123",
                uid = "uid-1",
                familyInformation = FamilyInformation(
                    familyId = "family-123",
                    members = listOf(
                        FamilyMember(uid = "uid-1", name = "Ane")
                    ),
                ),
            ),
            bitmap = null,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
@Preview(showBackground = true)
@Composable
private fun FamilyScreenEditingPreview() {
    LifeTogetherTheme {
        FamilyScreen(
            uiState = FamilyUiState.Content(
                familyId = "family-123",
                uid = "uid-1",
                familyInformation = FamilyInformation(
                    familyId = "family-123",
                    members = listOf(
                        FamilyMember(uid = "uid-1", name = "Ane")
                    ),
                ),
                isTogetherSinceEditing = true,
                togetherSinceDraft = Date(),
            ),
            bitmap = null,
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
