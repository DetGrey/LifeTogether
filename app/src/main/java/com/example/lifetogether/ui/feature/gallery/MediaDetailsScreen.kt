package com.example.lifetogether.ui.feature.gallery

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toAbbreviatedDateString
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.ui.common.OverflowMenu
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.CustomAlertDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.DisplayImageFromUri
import com.example.lifetogether.ui.common.image.DisplayVideoFromUri
import com.example.lifetogether.ui.common.image.MediaInfoPanel
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.domain.sync.SyncKey

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsScreen(
    appNavigator: AppNavigator? = null,
    albumId: String,
    initialIndex: Int,
) {
    val mediaDetailsViewModel: MediaDetailsViewModel = hiltViewModel()
    val uiState by mediaDetailsViewModel.uiState.collectAsState()

    LaunchedEffect(albumId) {
        mediaDetailsViewModel.setUp(albumId, initialIndex)
    }

    val mediaList = uiState.mediaList
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaList.size }
    )

    // TODO is this needed since it is empty?
    // Pause videos when swiping away from them
    LaunchedEffect(pagerState.currentPage) {
        // This will be called whenever the page changes
        // Video players in non-visible pages will pause via lifecycle
    }

    val containerSize = LocalWindowInfo.current.containerSize

    // Animate the "snap" when the user lets go
    val animatedOffset by animateFloatAsState(
        targetValue = uiState.offsetY,
        label = "offsetAnimation",
        animationSpec = spring(stiffness = Spring.StiffnessLow) // Makes it feel premium
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(containerSize) { // Re-bind if size changes
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        mediaDetailsViewModel.onVerticalDrag(dragAmount, containerSize.height)
                    },
                    onDragEnd = {
                        mediaDetailsViewModel.onDragEnd(containerSize.height)
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    appNavigator?.navigateBack()
                },
                text = mediaList.getOrNull(pagerState.currentPage)
                    ?.dateCreated
                    ?.toAbbreviatedDateString()
                    ?: "Media",
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { mediaDetailsViewModel.toggleOverflowMenu() },
            )

            SyncUpdatingText(
                keys = setOf(SyncKey.GALLERY_ALBUMS, SyncKey.GALLERY_MEDIA),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val media = mediaList[page]

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    when (media) {
                        is GalleryImage -> {
                            media.mediaUri?.let {
                                DisplayImageFromUri(it, media.itemName)
                            }
                        }
                        is GalleryVideo -> {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4f) // This is our panel height
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = animatedOffset + (size.height) // Offset it by its own height to hide it
                }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            mediaList.getOrNull(pagerState.currentPage)?.let {
                MediaInfoPanel(it)
            }
        }
    }

    // ---------------------------------------------------------------- DOWNLOAD
    uiState.downloadMessage?.let { message ->
        CustomAlertDialog(
            title = if (uiState.isDownloading) "Downloading..." else "Finished downloading",
            details = message,
            extraContent = {
                if (uiState.isDownloading) {
                    CircularProgressIndicator()
                }
            }
        )
    }

    // ---------------------------------------------------------------- OVERFLOW MENU
    if (uiState.showOverflowMenu) {
        OverflowMenu(
            onDismiss = { mediaDetailsViewModel.toggleOverflowMenu() },
            actionsList = MenuAction.MediaDetailsActions.entries.map {
                mapOf(it.label to { mediaDetailsViewModel.startOverflowAction(it) })
            }
        )
    }

    // ---------------------------------------------------------------- OVERFLOW MENU ACTIONS DIALOG
    if (uiState.showOverflowMenuActionDialog && uiState.overflowMenuAction != null) {
        when (uiState.overflowMenuAction) {
            MenuAction.MediaDetailsActions.DOWNLOAD -> {
                if (!uiState.isDownloading && mediaList.isNotEmpty()) {
                    mediaDetailsViewModel.downloadMedia(pagerState.currentPage)

                }
            }
            MenuAction.MediaDetailsActions.DELETE -> {
                ConfirmationDialog(
                    onDismiss = { mediaDetailsViewModel.dismissOverflowMenuActionDialog() },
                    onConfirm = {
                        mediaDetailsViewModel.deleteMedia(pagerState.currentPage)
                    },
                    dialogTitle = "Delete",
                    dialogMessage = "Are you sure you want to delete this?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete",
                )
            }
            else -> {}
        }
    }
    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (uiState.showAlertDialog) {
        LaunchedEffect(uiState.error) {
            mediaDetailsViewModel.dismissAlert()
        }
        ErrorAlertDialog(uiState.error)
    }
}
