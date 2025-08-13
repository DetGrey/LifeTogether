package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.DisplayImageFromUri
import com.example.lifetogether.ui.common.image.DisplayVideoFromUri
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryMediaScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    mediaId: String,
) {
    val galleryMediaViewModel: GalleryMediaViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val imageData by galleryMediaViewModel.mediaData.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        userInformation?.familyId?.let {
            galleryMediaViewModel.setUpMediaData(it, mediaId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    appNavigator?.navigateBack()
                },
                text = when (imageData?.mediaType) {
                    MediaType.IMAGE -> "Image"
                    MediaType.VIDEO -> "Video"
                    else -> "Media"
                },
            )
            if (imageData?.mediaUri != null) {
                if (imageData?.mediaType == MediaType.IMAGE) {
                    DisplayImageFromUri(
                        imageUri = imageData?.mediaUri!!,
                        description = imageData?.itemName,
                    )
                } else if (imageData?.mediaType == MediaType.VIDEO) {
                    DisplayVideoFromUri(
                        videoUri = imageData?.mediaUri!!,
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (galleryMediaViewModel.showAlertDialog) {
        ErrorAlertDialog(galleryMediaViewModel.error)
        galleryMediaViewModel.toggleAlertDialog()
    }
}
