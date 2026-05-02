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
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.enums.SettingsConfirmationTypes
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onUiEvent: (SettingsUiEvent) -> Unit,
    onNavigationEvent: (SettingsNavigationEvent) -> Unit,
) {
    val userInformationState = uiState.userInformation

    Scaffold(
        topBar = {
            AppTopBar(
                leftIcon = Icon(
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
                        icon = Icon(R.drawable.ic_profile_picture, "profile icon"),
                        title = userInformationState?.name ?: "Username",
                        link = "Edit my profile",
                        linkClickable = {
                            onNavigationEvent(SettingsNavigationEvent.NavigateToProfile)
                        },
                    )

                    if (userInformationState?.familyId is String) {
                        SettingsItem(
                            icon = Icon(R.drawable.ic_family, "family icon"),
                            title = "My family",
                            link = "Edit family",
                            linkClickable = {
                                onNavigationEvent(SettingsNavigationEvent.NavigateToFamily)
                            },
                        )
                    } else {
                        SettingsItem(
                            icon = Icon(R.drawable.ic_family, "family icon"),
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
                        icon = Icon(R.drawable.ic_bell, "bell icon"),
                        title = "Notifications",
                        link = "Manage notifications",
                        linkClickable = null,
                    )
                }
            }
        }

        if (uiState.showConfirmationDialog) {
            when (uiState.confirmationDialogType) {
                SettingsConfirmationTypes.JOIN_FAMILY -> ConfirmationDialogWithTextField(
                    onDismiss = {
                        onUiEvent(SettingsUiEvent.DismissConfirmationDialog)
                    },
                    onConfirm = {
                        onUiEvent(SettingsUiEvent.ConfirmJoinFamily)
                    },
                    dialogTitle = "Join a family",
                    dialogMessage = "Please add the family id to join",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Join",
                    textValue = uiState.addedFamilyId,
                    onTextValueChange = { value ->
                        onUiEvent(SettingsUiEvent.AddedFamilyIdChanged(value))
                    },
                    label = "Family id",
                )

                SettingsConfirmationTypes.NEW_FAMILY -> ConfirmationDialog(
                    onDismiss = {
                        onUiEvent(SettingsUiEvent.DismissConfirmationDialog)
                    },
                    onConfirm = {
                        onUiEvent(SettingsUiEvent.ConfirmCreateNewFamily)
                    },
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
fun SettingsScreenPreview() {
    LifeTogetherTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                userInformation = UserInformation(
                    name = "Alex",
                ),
                confirmationDialogType = null,
                showConfirmationDialog = false,
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
