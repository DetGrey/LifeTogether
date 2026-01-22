package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
    albumId: String,
    initialIndex: Int,
) {
    val galleryMediaViewModel: GalleryMediaViewModel = hiltViewModel()
    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val uiState by galleryMediaViewModel.uiState.collectAsState()

    LaunchedEffect(albumId) {
        userInformation?.familyId?.let {
            galleryMediaViewModel.setUpMediaData(it, albumId, initialIndex)
        }
    }

    val mediaList = uiState.mediaList
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaList.size }
    )

    // Pause videos when swiping away from them
    LaunchedEffect(pagerState.currentPage) {
        // This will be called whenever the page changes
        // Video players in non-visible pages will pause via lifecycle
    }

    Box(modifier = Modifier
        .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                text = if (mediaList.isNotEmpty()) {
                    val currentMedia = mediaList.getOrNull(pagerState.currentPage)
                    when (currentMedia?.mediaType) {
                        MediaType.IMAGE -> "Image ${pagerState.currentPage + 1}/${mediaList.size}"
                        MediaType.VIDEO -> "Video ${pagerState.currentPage + 1}/${mediaList.size}"
                        else -> "Media ${pagerState.currentPage + 1}/${mediaList.size}"
                    }
                } else {
                    "Media"
                },
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "download menu",
                ),
                onRightClick = {
                    if (!uiState.isDownloading && mediaList.isNotEmpty()) {
                        galleryMediaViewModel.downloadMedia(pagerState.currentPage)
                    }
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                pageSpacing = 16.dp, // Better UX for horizontal images
                beyondViewportPageCount = 1 // Pre-loads for performance
            ) { page ->
                val media = mediaList[page]

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (media.mediaType) {
                        MediaType.IMAGE -> {
                            media.mediaUri?.let { uri ->
                                DisplayImageFromUri(
                                    imageUri = uri,
                                    description = media.itemName,
                                )
                            }
                        }
                        MediaType.VIDEO -> {
                            media.mediaUri?.let { uri ->
                                DisplayVideoFromUri(
                                    videoUri = uri,
                                    autoPlay = false, // User taps to play
                                    useController = true,
                                    modifier = Modifier.fillMaxSize(),
                                    keepScreenOn = true
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- DOWNLOAD
        if (uiState.downloadMessage != null) {
            Box { // TODO make something for this??
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
