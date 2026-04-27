package com.example.lifetogether.ui.feature.profile

import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.text.TextHeadingLarge
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    bitmap: Bitmap?,
    isAdmin: Boolean,
    showImageUploadDialog: Boolean,
    onImageUpload: suspend (Uri) -> Result<Unit, AppError>,
    onUiEvent: (ProfileUiEvent) -> Unit,
    onNavigationEvent: (ProfileNavigationEvent) -> Unit,
) {
    val userInformation = uiState.userInformation

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
                        onNavigationEvent(ProfileNavigationEvent.NavigateBack)
                    },
                    text = "Profile",
                    rightIcon = Icon(
                        resId = R.drawable.ic_settings,
                        description = "settings icon",
                    ),
                    onRightClick = {
                        onNavigationEvent(ProfileNavigationEvent.NavigateToSettings)
                    },
                )
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier.size(250.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(shape = CircleShape)
                                .background(color = MaterialTheme.colorScheme.onBackground),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (bitmap != null) {
                                Image(
                                    modifier = Modifier.fillMaxSize(),
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "profile picture",
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                androidx.compose.material3.Icon(
                                    modifier = Modifier.fillMaxSize(),
                                    painter = painterResource(id = R.drawable.ic_avatar),
                                    contentDescription = "profile picture",
                                    tint = Color.Unspecified
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(end = LifeTogetherTokens.spacing.small)
                                .size(LifeTogetherTokens.spacing.xxxLarge),
                            contentAlignment = Alignment.Center,
                        ) {
                            AddButton(onClick = { onUiEvent(ProfileUiEvent.AddImageClicked) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))
                TextHeadingLarge(text = userInformation?.name ?: "")
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                ) {
                    TextHeadingMedium(text = "Personal details")

                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_profile,
                            description = "person icon",
                        ),
                        title = "Name",
                        value = userInformation?.name ?: "",
                        onClick = {
                            onUiEvent(ProfileUiEvent.NameClicked)
                        },
                    )
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_email,
                            description = "at sign icon",
                        ),
                        title = "Email",
                        value = userInformation?.email ?: "",
                    )
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_cake,
                            description = "cake icon",
                        ),
                        title = "Birthday",
                        value = userInformation?.birthday?.toFullDateString() ?: "",
                    )
                    if (isAdmin) {
                        ProfileDetails(
                            icon = Icon(
                                resId = R.drawable.ic_settings,
                                description = "settings icon",
                            ),
                            title = "User type",
                            value = "Admin",
                        )
                    }
                    // TODO: Implement password change flow and re-enable this row.
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_password,
                            description = "password icon",
                        ),
                        title = "Password",
                        value = "Change password",
                        enabled = false,
                    )
                    ProfileDetails(
                        icon = Icon(
                            resId = R.drawable.ic_logout,
                            description = "logout icon",
                        ),
                        title = "Logout",
                        value = "Logout",
                        onClick = {
                            onUiEvent(ProfileUiEvent.LogoutClicked)
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
                ProfileConfirmationType.LOGOUT -> ConfirmationDialog(
                    onDismiss = { onUiEvent(ProfileUiEvent.DismissConfirmationDialog) },
                    onConfirm = { onUiEvent(ProfileUiEvent.ConfirmConfirmationDialog) },
                    dialogTitle = "Logout",
                    dialogMessage = "Are you sure you want to logout?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Logout",
                )

                ProfileConfirmationType.NAME -> ConfirmationDialogWithTextField(
                    onDismiss = { onUiEvent(ProfileUiEvent.DismissConfirmationDialog) },
                    onConfirm = { onUiEvent(ProfileUiEvent.ConfirmConfirmationDialog) },
                    dialogTitle = "Change name",
                    dialogMessage = "Please enter your new name",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Change name",
                    textValue = uiState.newName,
                    onTextValueChange = { value ->
                        onUiEvent(ProfileUiEvent.NewNameChanged(value))
                    },
                    capitalization = true,
                )

                null -> Unit
            }
        }
    }

    if (showImageUploadDialog && userInformation?.uid != null) {
        ImageUploadDialog(
            onDismiss = { onUiEvent(ProfileUiEvent.ImageUploadDismissed) },
            onConfirm = { onUiEvent(ProfileUiEvent.ImageUploadConfirmed) },
            onUpload = onImageUpload,
            dialogTitle = "Upload profile photo",
            dialogMessage = "Select your new profile photo",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Upload photo",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    LifeTogetherTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                userInformation = UserInformation(
                    uid = "uid-1",
                    name = "Alex",
                    email = "alex@example.com",
                ),
            ),
            bitmap = null,
            isAdmin = true,
            showImageUploadDialog = false,
            onImageUpload = { Result.Success(Unit) },
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
