package com.example.lifetogether.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.domain.logic.copyToClipboard
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.add.AddNewString
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUiEvent: (SettingsUiEvent) -> Unit,
    onNavigationEvent: (SettingsNavigationEvent) -> Unit,
) {
    val context = LocalContext.current
    var showFamilyOptionsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(SettingsNavigationEvent.NavigateBack)
                },
                text = "Settings",
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is SettingsUiState.Loading,
            label = "settings_loading",
            loadingContent = {
                Skeletons.ListDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = uiState as? SettingsUiState.Content ?: return@AnimatedLoadingContent
            val userInformationState = content.userInformation

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
                        SettingsItem(
                            appIcon = AppIcon(R.drawable.ic_profile_picture, "profile icon"),
                            title = userInformationState.name,
                            link = "Edit my profile",
                            isLinkClickable = true,
                            onClick = {
                                onNavigationEvent(SettingsNavigationEvent.NavigateToProfile)
                            },
                        )

                        if (userInformationState.familyId != null) {
                            SettingsItem(
                                appIcon = AppIcon(R.drawable.ic_family, "family icon"),
                                title = "My family",
                                link = "Edit family",
                                isLinkClickable = true,
                                onClick = {
                                    onNavigationEvent(SettingsNavigationEvent.NavigateToFamily)
                                },
                            )
                        } else {
                            SettingsItem(
                                appIcon = AppIcon(R.drawable.ic_family, "family icon"),
                                title = "Join a family",
                                isTitleClickable = true,
                                link = "Create new family",
                                isLinkClickable = true,
                                onClick = {
                                    showFamilyOptionsSheet = true
                                },
                            )
                        }

                        SettingsItem(
                            appIcon = AppIcon(R.drawable.ic_bell, "bell icon"),
                            title = "Notifications",
                            link = "Manage notifications",
                            isLinkClickable = true,
                            onClick = {
                                onNavigationEvent(SettingsNavigationEvent.NavigateToNotifications)
                            },
                        )
                    }
                }

                if (content.isAdmin) {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                        ) {
                            TextHeadingMedium(text = "Admin access")

                            content.adminUids.forEach { adminUid ->
                                AdminUidRow(
                                    uid = adminUid,
                                    onRemoveClick = {
                                        onUiEvent(SettingsUiEvent.RemoveAdminClicked(adminUid))
                                    },
                                )
                            }

                            AddNewString(
                                label = "Add User ID",
                                textValue = content.adminUidDraft,
                                onTextChange = { value ->
                                    onUiEvent(SettingsUiEvent.AdminUidChanged(value))
                                },
                                onAddClick = { _ ->
                                    onUiEvent(SettingsUiEvent.AddAdminClicked)
                                },
                                capitalization = false,
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(top = LifeTogetherTokens.spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                    ) {
                        TextDefault(text = "Version ${BuildConfig.VERSION_NAME}")
                        TextDefault(
                            text = "User ID: ${userInformationState.uid}",
                            modifier = Modifier.clickable {
                                copyToClipboard(
                                    context = context,
                                    text = userInformationState.uid,
                                )
                            }
                        )
                    }
                }
            }

            when (val dialog = content.dialog) {
                is SettingsDialogState.JoinFamily -> ConfirmationDialogWithTextField(
                    onDismiss = { onUiEvent(SettingsUiEvent.DismissDialog) },
                    onConfirm = { onUiEvent(SettingsUiEvent.ConfirmJoinFamily) },
                    dialogTitle = "Join a family",
                    dialogMessage = "Please add the family id to join",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Join",
                    textValue = dialog.familyId,
                    onTextValueChange = { value ->
                        onUiEvent(SettingsUiEvent.FamilyIdChanged(value))
                    },
                    label = "Family id",
                )

                is SettingsDialogState.CreateFamily -> ConfirmationDialog(
                    onDismiss = { onUiEvent(SettingsUiEvent.DismissDialog) },
                    onConfirm = { onUiEvent(SettingsUiEvent.ConfirmCreateNewFamily) },
                    dialogTitle = "Create new family",
                    dialogMessage = "Are you sure you want to create a new family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Create",
                )

                is SettingsDialogState.RemoveAdmin -> ConfirmationDialog(
                    onDismiss = { onUiEvent(SettingsUiEvent.DismissDialog) },
                    onConfirm = { onUiEvent(SettingsUiEvent.ConfirmRemoveAdmin) },
                    dialogTitle = "Remove admin access",
                    dialogMessage = "Are you sure you want to remove admin access for ${dialog.uid}?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Remove",
                )

                null -> Unit
            }
        }
    }

    if (showFamilyOptionsSheet) {
        ActionSheet(
            onDismiss = { showFamilyOptionsSheet = false },
            actionsList = listOf(
                ActionSheetItem(
                    label = "Join a family",
                    onClick = {
                        showFamilyOptionsSheet = false
                        onUiEvent(SettingsUiEvent.JoinFamilyClicked)
                    },
                ),
                ActionSheetItem(
                    label = "Create new family",
                    onClick = {
                        showFamilyOptionsSheet = false
                        onUiEvent(SettingsUiEvent.CreateNewFamilyClicked)
                    },
                ),
            ),
        )
    }
}

@Composable
private fun AdminUidRow(
    uid: String,
    onRemoveClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = LifeTogetherTokens.spacing.medium,
                    vertical = LifeTogetherTokens.spacing.small
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextDefault(
                text = uid,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(LifeTogetherTokens.sizing.iconLarge),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete),
                    contentDescription = "remove admin access",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    LifeTogetherTheme {
        SettingsScreen(
            uiState = SettingsUiState.Content(
                userInformation = UserInformation(
                    uid = "user-1",
                    email = "alex@example.com",
                    name = "Alex",
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
