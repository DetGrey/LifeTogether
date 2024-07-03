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
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
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
//    val userInformation = UserInformation()

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
                                // TODO
                            },
                        )
                    } else {
                        SettingsItem(
                            icon = Icon(R.drawable.ic_profile_picture, "profile pic"),
                            title = "Connect to family",
                            titleClickable = {
                                // TODO
                            },
                            link = "Create new family",
                            linkClickable = {
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
            ConfirmationDialog(
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
