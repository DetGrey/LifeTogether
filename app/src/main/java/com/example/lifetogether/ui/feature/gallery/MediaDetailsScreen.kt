package com.example.lifetogether.ui.feature.gallery

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.durationToString
import com.example.lifetogether.domain.logic.toAbbreviatedDateString
import com.example.lifetogether.domain.logic.toDateTimeString
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.image.DisplayImageFromUri
import com.example.lifetogether.ui.common.image.DisplayVideoFromUri
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

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

    Scaffold(
        topBar = {
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
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(containerSize) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            onUiEvent(MediaDetailsUiEvent.VerticalDrag(dragAmount, containerSize.height))
                        },
                        onDragEnd = {
                            onUiEvent(MediaDetailsUiEvent.DragEnd(containerSize.height))
                        }
                    )
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LifeTogetherTokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f) // This is our panel height
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (animatedOffset + (containerSize.height * 0.4f)).roundToInt(),
                        )
                    },
                shape = MaterialTheme.shapes.extraLarge.copy(
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp),
                ),
                color = MaterialTheme.colorScheme.surface,
            ) {
                mediaList.getOrNull(pagerState.currentPage)?.let {
                    MediaDetailsPanelContent(it)
                }
            }
        }
    }

    if (uiState.showOverflowMenu) {
        ActionSheet(
            onDismiss = { onUiEvent(MediaDetailsUiEvent.ToggleOverflowMenu) },
            actionsList = MenuAction.MediaDetailsActions.entries.map {
                ActionSheetItem(
                    label = it.label,
                    onClick = { onUiEvent(MediaDetailsUiEvent.StartOverflowAction(it)) },
                    isDestructive = it == MenuAction.MediaDetailsActions.DELETE,
                )
            },
        )
    }

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

@Composable
private fun MediaDetailsPanelContent(media: GalleryMedia) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextDefault(
            text = media.dateCreated?.toDateTimeString() ?: "Unknown date",
        )

        if (media is GalleryVideo) {
            TextDefault(
                text = media.duration?.durationToString() ?: "Unknown duration",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaDetailsScreenPreview() {
    LifeTogetherTheme {
        MediaDetailsScreen(
            uiState = MediaDetailsUiState(
                mediaList = listOf(
                    GalleryImage(
                        id = "media-1",
                        familyId = "family-1",
                        itemName = "Kitchen shelf",
                        lastUpdated = Date(1_717_200_000_000),
                        albumId = "album-1",
                        dateCreated = Date(1_717_200_000_000),
                    ),
                    GalleryVideo(
                        id = "media-2",
                        familyId = "family-1",
                        itemName = "Living room",
                        lastUpdated = Date(1_717_200_000_000),
                        albumId = "album-1",
                        dateCreated = Date(1_717_200_000_000),
                        duration = 42_000L,
                    ),
                ),
                currentIndex = 0,
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
