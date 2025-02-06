package com.example.lifetogether.ui.feature.family

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.converter.copyToClipboard
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.feature.profile.ProfileDetails
import com.example.lifetogether.ui.feature.settings.SettingsItem
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FamilyViewModel
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.example.lifetogether.ui.viewmodel.ImageViewModel

@Composable
fun FamilyScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
) {
    val context = LocalContext.current
    val familyViewModel: FamilyViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()

    val userInformationState by firebaseViewModel?.userInformation!!.collectAsState()
    val familyInformationState by familyViewModel.familyInformation.collectAsState()
    val bitmap by imageViewModel.bitmap.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        println("FamilyScreen familyId: ${userInformationState?.familyId}")
        userInformationState?.familyId?.let { familyViewModel.setUpFamilyInformation(it) }

        userInformationState?.familyId?.let { familyId ->
            imageViewModel.collectImageFlow(
                imageType = ImageType.FamilyImage(familyId),
                onError = {
                    familyViewModel.error = it
                    familyViewModel.showAlertDialog = true
                },
            )
        }
    }

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
                    text = "Family",
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier,
                            contentAlignment = Alignment.TopEnd,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 15.dp)
                                    .height(200.dp)
                                    .clip(shape = RoundedCornerShape(20))
                                    .background(color = MaterialTheme.colorScheme.onBackground),
                            ) {
                                if (bitmap != null) {
                                    Image(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        bitmap = bitmap!!.asImageBitmap(),
                                        contentDescription = "family image",
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(50.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AddButton(onClick = { imageViewModel.showImageUploadDialog = true })
                            }
                        }
                    }

                    TextHeadingMedium(text = "Family members")

                    when (familyInformationState?.members) {
                        is List<FamilyMember> -> {
                            for (member in familyInformationState?.members!!) {
                                ProfileDetails(
                                    icon = Icon(
                                        resId = R.drawable.ic_profile,
                                        description = "person icon",
                                    ),
                                    title = member.name ?: "Unknown Member",
                                    value = if (member.uid == userInformationState?.uid) "(Myself)" else "Remove",
                                    onClick = if (member.uid == userInformationState?.uid) {
                                        null
                                    } else {
                                        {
                                            familyViewModel.confirmationDialogType = FamilyViewModel.FamilyConfirmationTypes.REMOVE_MEMBER
                                            familyViewModel.memberToRemove = member
                                            familyViewModel.showConfirmationDialog = true
                                        }
                                    },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TextHeadingMedium(text = "Settings")

                    SettingsItem(
                        icon = Icon(R.drawable.ic_profile, "profile icon"),
                        title = "Add family member",
                        link = "Share family ID",
                        linkClickable = {
                            familyViewModel.confirmationDialogType = FamilyViewModel.FamilyConfirmationTypes.ADD_MEMBER
                            familyViewModel.showConfirmationDialog = true
                        },
                    )

                    SettingsItem(
                        icon = Icon(R.drawable.ic_logout, "logout icon"),
                        title = "Leave family",
                        link = "Leave",
                        linkClickable = {
                            familyViewModel.confirmationDialogType = FamilyViewModel.FamilyConfirmationTypes.LEAVE_FAMILY
                            familyViewModel.showConfirmationDialog = true
                        },
                    )

                    SettingsItem(
                        icon = Icon(R.drawable.ic_logout, "logout icon"),
                        title = "Delete family",
                        link = "Delete",
                        linkClickable = {
                            familyViewModel.confirmationDialogType = FamilyViewModel.FamilyConfirmationTypes.DELETE_FAMILY
                            familyViewModel.showConfirmationDialog = true
                        },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // ---------------------------------------------------------------- CONFIRMATION DIALOGS
        if (familyViewModel.showConfirmationDialog) {
            when (familyViewModel.confirmationDialogType) {
                FamilyViewModel.FamilyConfirmationTypes.LEAVE_FAMILY -> ConfirmationDialog(
                    onDismiss = { familyViewModel.closeConfirmationDialog() },
                    onConfirm = {
                        userInformationState?.uid.let { uid ->
                            userInformationState?.familyId.let { familyId ->
                                if (uid != null && familyId != null) {
                                    familyViewModel.leaveFamily(familyId, uid, onComplete = {
                                        appNavigator?.navigateBack()
                                    })
                                }
                            }
                        }
                    },
                    dialogTitle = "Leave family",
                    dialogMessage = "Are you sure you want to leave the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Leave",
                )

                FamilyViewModel.FamilyConfirmationTypes.ADD_MEMBER -> ConfirmationDialog(
                    onDismiss = { familyViewModel.closeConfirmationDialog() },
                    onConfirm = {
                        userInformationState?.familyId.let { familyId ->
                            if (familyId != null) {
                                copyToClipboard(context, familyId)
                                familyViewModel.closeConfirmationDialog()
                            }
                        }
                    },
                    dialogTitle = "Share family ID",
                    dialogMessage = "Family ID: ${userInformationState?.familyId}",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Copy",
                )

                FamilyViewModel.FamilyConfirmationTypes.REMOVE_MEMBER -> ConfirmationDialog(
                    onDismiss = { familyViewModel.closeConfirmationDialog() },
                    onConfirm = {
                        familyViewModel.memberToRemove.let { member ->
                            userInformationState?.familyId.let { familyId ->
                                if (member?.uid != null && familyId != null) {
                                    familyViewModel.leaveFamily(familyId, member.uid, {})
                                }
                            }
                        }
                    },
                    dialogTitle = "Remove member",
                    dialogMessage = "Are you sure you want to remove ${familyViewModel.memberToRemove?.name} from the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Remove",
                )

                FamilyViewModel.FamilyConfirmationTypes.DELETE_FAMILY -> ConfirmationDialog(
                    onDismiss = { familyViewModel.closeConfirmationDialog() },
                    onConfirm = {
                        userInformationState?.familyId.let { familyId ->
                            if (familyId != null) {
                                familyViewModel.deleteFamily(familyId, onComplete = {
                                    appNavigator?.navigateBack()
                                })
                            }
                        }
                    },
                    dialogTitle = "Delete family",
                    dialogMessage = "Are you sure you want to delete the family?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete",
                )

                null -> {}
            }
        }
    }

    // ---------------------------------------------------------------- IMAGE UPLOAD DIALOG
    if (imageViewModel.showImageUploadDialog) {
        familyInformationState?.familyId?.let {
            ImageUploadDialog(
                onDismiss = { imageViewModel.showImageUploadDialog = false },
                onConfirm = { imageViewModel.showImageUploadDialog = false },
                dialogTitle = "Upload family image",
                dialogMessage = "Select your new family image",
                imageType = ImageType.FamilyImage(it),
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload image",
            )
        }
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (familyViewModel.showAlertDialog) {
        ErrorAlertDialog(familyViewModel.error)
        familyViewModel.toggleAlertDialog()
    }
}

@Preview(showBackground = true)
@Composable
fun FamilyScreenPreview() {
    LifeTogetherTheme {
        FamilyScreen()
    }
}
