package com.example.lifetogether.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUiEvent: (SettingsUiEvent) -> Unit,
    onNavigationEvent: (SettingsNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
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
                            linkClickable = {
                                onNavigationEvent(SettingsNavigationEvent.NavigateToProfile)
                            },
                        )

                        if (userInformationState.familyId != null) {
                            SettingsItem(
                                appIcon = AppIcon(R.drawable.ic_family, "family icon"),
                                title = "My family",
                                link = "Edit family",
                                linkClickable = {
                                    onNavigationEvent(SettingsNavigationEvent.NavigateToFamily)
                                },
                            )
                        } else {
                            SettingsItem(
                                appIcon = AppIcon(R.drawable.ic_family, "family icon"),
                                title = "Join a family",
                                titleClickable = {
                                    onUiEvent(SettingsUiEvent.JoinFamilyClicked)
                                },
                                link = "Create new family",
                                linkClickable = {
                                    onUiEvent(SettingsUiEvent.CreateNewFamilyClicked)
                                },
                            )
                        }

                        SettingsItem(
                            appIcon = AppIcon(R.drawable.ic_bell, "bell icon"),
                            title = "Notifications",
                            link = "Manage notifications",
                            linkClickable = null,
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(top = LifeTogetherTokens.spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                    ) {
                        TextDefault(text = "Version ${BuildConfig.VERSION_NAME}")
                        TextDefault(text = "User ID: ${userInformationState.uid}")
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

                null -> Unit
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
