package com.example.lifetogether.ui.feature.profile

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.ConfirmationDialogDetails
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.ConfirmationDialog
import com.example.lifetogether.ui.common.ProfileDetails
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.convert.formatDateToString
import com.example.lifetogether.ui.common.text.TextHeadingLarge
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AuthViewModel
import com.example.lifetogether.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    appNavigator: AppNavigator? = null,
    authViewModel: AuthViewModel? = null,
) {
    val profileViewModel: ProfileViewModel = viewModel()

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
                    text = "Profile",
                    rightIcon = Icon(
                        resId = R.drawable.ic_settings,
                        description = "settings icon",
                    ),
                    onRightClick = {
                        appNavigator?.navigateToSettings()
                    },
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape = RoundedCornerShape(100))
                                .background(color = MaterialTheme.colorScheme.onBackground),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                modifier = Modifier
                                    .fillMaxSize(),
                                painter = painterResource(id = R.drawable.profile_picture), // TODO update to real picture if added
                                contentDescription = "profile picture",
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .size(60.dp)
                                .clip(shape = RoundedCornerShape(100))
                                .background(color = MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = "+", fontSize = 30.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextHeadingLarge(text = authViewModel?.userInformation?.name ?: "")
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    TextHeadingMedium(text = "Personal details")

                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_profile, // TODO
                            description = "", // TODO
                        ),
                        title = "Name",
                        value = authViewModel?.userInformation?.name ?: "",
                        onClick = {
                            profileViewModel.confirmationDialogType = ProfileViewModel.ConfirmationType.NAME
                            profileViewModel.showConfirmationDialog = true
                        },
                    )
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_profile, // TODO
                            description = "", // TODO
                        ),
                        title = "Email",
                        value = authViewModel?.userInformation?.email ?: "",
                    )
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_profile, // TODO
                            description = "", // TODO
                        ),
                        title = "Birthday",
                        value = authViewModel?.userInformation?.birthday?.let { date ->
                            formatDateToString(date)
                        } ?: "",
                    )
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_profile, // TODO
                            description = "", // TODO
                        ),
                        title = "Password",
                        value = "Change password",
                        onClick = {
                            profileViewModel.confirmationDialogType = ProfileViewModel.ConfirmationType.PASSWORD
                            profileViewModel.showConfirmationDialog = true
                        },
                    )
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_logout,
                            description = "logout icon",
                        ),
                        title = "Logout",
                        value = "Logout",
                        onClick = {
                            profileViewModel.confirmationDialogType = ProfileViewModel.ConfirmationType.LOGOUT
                            profileViewModel.showConfirmationDialog = true
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        if (profileViewModel.showConfirmationDialog) {
            val confirmationDialogDetails = when (profileViewModel.confirmationDialogType) {
                ProfileViewModel.ConfirmationType.LOGOUT -> ConfirmationDialogDetails(
                    dialogTitle = "Logout",
                    dialogMessage = "Are you sure you want to logout?",
                    confirmButtonMessage = "Logout",
                    onConfirm = {
                        profileViewModel.logout(
                            onSuccess = {
                                authViewModel?.updateUserInformation(null)
                                profileViewModel.showConfirmationDialog = false
                                appNavigator?.navigateToHome()
                            },
                        )
                    },
                )

                ProfileViewModel.ConfirmationType.NAME -> ConfirmationDialogDetails(
                    dialogTitle = "Change name",
                    dialogMessage = "Are you sure you want to change name?", // TODO
                    confirmButtonMessage = "Change name",
                    onConfirm = {}, // TODO
                )

                ProfileViewModel.ConfirmationType.PASSWORD -> ConfirmationDialogDetails(
                    dialogTitle = "Change password",
                    dialogMessage = "Are you sure you want change password?", // TODO
                    confirmButtonMessage = "Change password",
                    onConfirm = {}, // TODO
                )

                null -> null
            }

            if (confirmationDialogDetails != null) {
                ConfirmationDialog(
                    onDismiss = { profileViewModel.showConfirmationDialog = false },
                    onConfirm = {
                        confirmationDialogDetails.onConfirm
                    },
                    dialogTitle = confirmationDialogDetails.dialogTitle,
                    dialogMessage = confirmationDialogDetails.dialogMessage,
                    dismissButtonMessage = confirmationDialogDetails.dismissButtonMessage,
                    confirmButtonMessage = confirmationDialogDetails.confirmButtonMessage,
                )
            } else {
                profileViewModel.showConfirmationDialog = false
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    LifeTogetherTheme {
        ProfileScreen()
    }
}
