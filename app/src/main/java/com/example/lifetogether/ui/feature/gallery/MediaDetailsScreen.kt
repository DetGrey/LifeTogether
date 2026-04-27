package com.example.lifetogether.ui.feature.gallery

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toAbbreviatedDateString
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.ui.common.OverflowMenu
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.CustomAlertDialog
import com.example.lifetogether.ui.common.image.DisplayImageFromUri
import com.example.lifetogether.ui.common.image.DisplayVideoFromUri
import com.example.lifetogether.ui.common.image.MediaInfoPanel
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.domain.sync.SyncKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDetailsScreen(
    uiState: MediaDetailsUiState,
    onUiEvent: (MediaDetailsUiEvent) -> Unit,
    onNavigationEvent: (MediaDetailsNavigationEvent) -> Unit,
) {
    val mediaList = uiState.mediaList
    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { mediaList.size },
    )

    val containerSize = LocalWindowInfo.current.containerSize

    val animatedOffset by animateFloatAsState(
        targetValue = uiState.offsetY,
        label = "offsetAnimation",
        animationSpec = spring(stiffness = Spring.StiffnessLow),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(containerSize) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        onUiEvent(MediaDetailsUiEvent.VerticalDrag(dragAmount, containerSize.height))
                    },
                    onDragEnd = {
                        onUiEvent(MediaDetailsUiEvent.DragEnd(containerSize.height))
                    },
                )
            },
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
                    onNavigationEvent(MediaDetailsNavigationEvent.NavigateBack)
                },
                text = mediaList.getOrNull(pagerState.currentPage)
                    ?.dateCreated
                    ?.toAbbreviatedDateString()
                    ?: "Media",
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { onUiEvent(MediaDetailsUiEvent.ToggleOverflowMenu) },
            )

            SyncUpdatingText(
                keys = setOf(SyncKey.GALLERY_ALBUMS, SyncKey.GALLERY_MEDIA),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize(),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1,
            ) { page ->
                val media = mediaList[page]

                Box(
                    modifier = Modifier.fillMaxSize(),
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
                                    autoPlay = false,
                                    useController = true,
                                    modifier = Modifier.fillMaxSize(),
                                    keepScreenOn = true,
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
                .clip(MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)))
                .background(MaterialTheme.colorScheme.surface),
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
            },
        )
    }

    // ---------------------------------------------------------------- OVERFLOW MENU
    if (uiState.showOverflowMenu) {
        OverflowMenu(
            onDismiss = { onUiEvent(MediaDetailsUiEvent.ToggleOverflowMenu) },
            actionsList = MenuAction.MediaDetailsActions.entries.map {
                mapOf(it.label to { onUiEvent(MediaDetailsUiEvent.StartOverflowAction(it)) })
            },
        )
    }

    // ---------------------------------------------------------------- OVERFLOW MENU ACTIONS DIALOG
    if (uiState.showOverflowMenuActionDialog && uiState.overflowMenuAction != null) {
        when (uiState.overflowMenuAction) {
            MenuAction.MediaDetailsActions.DOWNLOAD -> {
                onUiEvent(MediaDetailsUiEvent.DownloadMedia(pagerState.currentPage))
            }

            MenuAction.MediaDetailsActions.DELETE -> {
                ConfirmationDialog(
                    onDismiss = { onUiEvent(MediaDetailsUiEvent.DismissOverflowMenuActionDialog) },
                    onConfirm = { onUiEvent(MediaDetailsUiEvent.DeleteMedia(pagerState.currentPage)) },
                    dialogTitle = "Delete",
                    dialogMessage = "Are you sure you want to delete this?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete",
                )
            }
        }
    }
}
