package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    val uiState by galleryMediaViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = mediaId) {
        // Re-run when mediaId changes (navigating to different media)
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
                text = when (uiState.mediaData?.mediaType) {
                    MediaType.IMAGE -> "Image"
                    MediaType.VIDEO -> "Video"
                    else -> "Media"
                },
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu, // TODO add download icon or more options
                    description = "overflow menu",
                ),
                onRightClick = {
                    if (!uiState.isDownloading && uiState.mediaData != null) {
                        galleryMediaViewModel.downloadMedia()
                    }
                }
            )
            if (uiState.mediaData?.mediaUri != null) {
                if (uiState.mediaData?.mediaType == MediaType.IMAGE) {
                    DisplayImageFromUri(
                        imageUri = uiState.mediaData?.mediaUri!!,
                        description = uiState.mediaData?.itemName,
                    )
                } else if (uiState.mediaData?.mediaType == MediaType.VIDEO) {
                    DisplayVideoFromUri(
                        videoUri = uiState.mediaData?.mediaUri!!,
                    )
                }
            }
        }
        // ---------------------------------------------------------------- DOWNLOAD
        if (uiState.downloadMessage != null) {
            Box {
                if (uiState.isDownloading) {
                    CircularProgressIndicator()
                    Text(text = uiState.downloadMessage!!)
                } else {
                    Text(text = uiState.downloadMessage!!)
                }
            }
        }
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (uiState.showAlertDialog) {
        ErrorAlertDialog(uiState.error)
        galleryMediaViewModel.dismissAlert()
    }
}
