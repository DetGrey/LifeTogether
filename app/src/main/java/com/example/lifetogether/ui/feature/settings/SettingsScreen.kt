package com.example.lifetogether.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.enums.SettingsConfirmationTypes
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel
import com.example.lifetogether.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
) {
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val userInformation = authViewModel?.userInformation?.collectAsState(initial = null)

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_back_arrow,
                        description = "back arrow icon",
                    ),
                    onLeftClick = {
                        appNavigator?.navigateBack()
                    },
                    text = "Settings",
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    SettingsItem(
                        icon = Icon(R.drawable.ic_profile_picture, "profile pic"),
                        title = userInformation?.value?.name ?: "Username",
                        link = "Edit my profile",
                        linkClickable = {
                            appNavigator?.navigateToProfile()
                        },
                    )

                    if (userInformation?.value?.familyId is String) {
                        SettingsItem(
                            icon = Icon(R.drawable.ic_profile_picture, "profile pic"),
                            title = "My family",
                            link = "Edit family",
                            linkClickable = {
                                settingsViewModel.confirmationDialogType = SettingsConfirmationTypes.EDIT_FAMILY
                                settingsViewModel.showConfirmationDialog = true
                            },
                        )
                    } else {
                        SettingsItem(
                            icon = Icon(R.drawable.ic_profile_picture, "profile pic"),
                            title = "Join a family",
                            titleClickable = {
                                settingsViewModel.confirmationDialogType = SettingsConfirmationTypes.JOIN_FAMILY
                                settingsViewModel.showConfirmationDialog = true
                            },
                            link = "Create new family",
                            linkClickable = {
                                settingsViewModel.confirmationDialogType = SettingsConfirmationTypes.NEW_FAMILY
                                settingsViewModel.showConfirmationDialog = true
                            },
                        )
                    }

                    SettingsItem(
                        icon = Icon(R.drawable.ic_profile_picture, "profile pic"),
                        title = "Notifications",
                        link = "Manage notifications",
                        linkClickable = {
                            // TODO
                        },
                    )
                }
            }
        }

        if (settingsViewModel.showConfirmationDialog) {
            when (settingsViewModel.confirmationDialogType) {
                SettingsConfirmationTypes.EDIT_FAMILY -> ConfirmationDialog(
                    onDismiss = { settingsViewModel.closeConfirmationDialog() },
                    onConfirm = {
                        userInformation?.value?.uid.let { uid ->
                            userInformation?.value?.familyId.let { familyId ->
                                if (uid != null && familyId != null) {
                                    settingsViewModel.leaveFamily(familyId, uid)
                                }
                            }
                        }
                    },
                    dialogTitle = "Leave family",
                    dialogMessage = "Are you sure you want to leave the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Leave",
                )

                SettingsConfirmationTypes.JOIN_FAMILY -> ConfirmationDialogWithTextField(
                    onDismiss = { settingsViewModel.closeConfirmationDialog() },
                    onConfirm = {
                        userInformation?.value?.uid.let { uid ->
                            if (uid != null) {
                                settingsViewModel.joinFamily(uid)
                            }
                        }
                    },
                    dialogTitle = "Join a family",
                    dialogMessage = "Please add the family id to join",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Join",
                    textValue = settingsViewModel.addedFamilyId,
                    onTextValueChange = { settingsViewModel.addedFamilyId = it },
                )

                SettingsConfirmationTypes.NEW_FAMILY -> ConfirmationDialog(
                    onDismiss = { settingsViewModel.closeConfirmationDialog() },
                    onConfirm = {
                        userInformation?.value?.uid.let { uid ->
                            println("uid: $uid")
                            if (uid != null) {
                                settingsViewModel.createNewFamily(uid)
                            }
                        }
                    },
                    dialogTitle = "Create new family",
                    dialogMessage = "Are you sure you want to create a new family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Create",
                )

                null -> {}
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    LifeTogetherTheme {
        SettingsScreen()
    }
}
