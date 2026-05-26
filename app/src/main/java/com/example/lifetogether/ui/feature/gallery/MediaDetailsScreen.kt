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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.durationToString
import com.example.lifetogether.domain.logic.toAbbreviatedDateString
import com.example.lifetogether.domain.logic.toDateTimeString
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.image.DisplayImageFromUri
import com.example.lifetogether.ui.common.image.DisplayVideoFromUri
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun MediaDetailsScreen(
    uiState: MediaDetailsUiState,
    onUiEvent: (MediaDetailsUiEvent) -> Unit,
    onNavigationEvent: (MediaDetailsNavigationEvent) -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteTargetIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            val content = uiState as? MediaDetailsUiState.Content
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    onNavigationEvent(MediaDetailsNavigationEvent.NavigateBack)
                },
                text = content?.mediaList?.getOrNull(content.currentIndex)
                    ?.dateCreated
                    ?.toAbbreviatedDateString()
                    ?: "Media",
                rightAppIcon = AppIcon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { onUiEvent(MediaDetailsUiEvent.ToggleOverflowMenu) },
            )
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is MediaDetailsUiState.Loading,
            label = "media_details_loading",
            loadingContent = {
                Skeletons.SectionDetail(modifier = Modifier.fillMaxSize())
            },
        ) {
            val content = uiState as? MediaDetailsUiState.Content ?: return@AnimatedLoadingContent
            val mediaList = content.mediaList
            val pagerState = rememberPagerState(
                initialPage = content.currentIndex,
                pageCount = { mediaList.size },
            )
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        onUiEvent(MediaDetailsUiEvent.PageChanged(page))
                    }
            }

            val containerSize = LocalWindowInfo.current.containerSize

            val animatedOffset by animateFloatAsState(
                targetValue = content.offsetY,
                label = "offsetAnimation",
                animationSpec = spring(stiffness = Spring.StiffnessLow),
            )

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
                        modifier = Modifier.fillMaxSize(),
                        pageSpacing = 16.dp,
                        beyondViewportPageCount = 1,
                    ) { page ->
                        val media = mediaList[page]

                        Box(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            when (media) {
                                is GalleryImage -> {
                                    if (media.mediaUri != null) {
                                        DisplayImageFromUri(media.mediaUri, media.itemName)
                                    } else {
                                        MissingMediaState(
                                            state = media.downloadState,
                                            onRetry = { onUiEvent(MediaDetailsUiEvent.RetryMedia(page)) },
                                        )
                                    }
                                }

                                is GalleryVideo -> {
                                    if (media.mediaUri != null) {
                                        DisplayVideoFromUri(
                                            videoUri = media.mediaUri,
                                            autoPlay = false,
                                            useController = true,
                                            modifier = Modifier.fillMaxSize(),
                                            keepScreenOn = true,
                                        )
                                    } else {
                                        MissingMediaState(
                                            state = media.downloadState,
                                            onRetry = { onUiEvent(MediaDetailsUiEvent.RetryMedia(page)) },
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
                        .fillMaxHeight(0.4f)
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    mediaList.getOrNull(pagerState.currentPage)?.let {
                        MediaDetailsPanelContent(it)
                    }
                }
            }
        }
    }

    if (uiState is MediaDetailsUiState.Content) {
        if (uiState.showOverflowMenu) {
            ActionSheet(
                onDismiss = { onUiEvent(MediaDetailsUiEvent.ToggleOverflowMenu) },
                actionsList = MenuAction.MediaDetailsActions.entries.map { action ->
                    ActionSheetItem(
                        label = action.label,
                        onClick = {
                            when (action) {
                                MenuAction.MediaDetailsActions.DOWNLOAD -> {
                                    onUiEvent(MediaDetailsUiEvent.ToggleOverflowMenu)
                                    onUiEvent(MediaDetailsUiEvent.DownloadMedia(uiState.currentIndex))
                                }
                                MenuAction.MediaDetailsActions.DELETE -> {
                                    onUiEvent(MediaDetailsUiEvent.ToggleOverflowMenu)
                                    deleteTargetIndex = uiState.currentIndex
                                    showDeleteDialog = true
                                }
                            }
                        },
                        isDestructive = action == MenuAction.MediaDetailsActions.DELETE,
                    )
                },
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmationDialog(
            onDismiss = {
                showDeleteDialog = false
                deleteTargetIndex = null
            },
            onConfirm = {
                val idx = deleteTargetIndex
                showDeleteDialog = false
                deleteTargetIndex = null
                onUiEvent(MediaDetailsUiEvent.DeleteMedia(idx))
            },
            dialogTitle = "Delete",
            dialogMessage = "Are you sure you want to delete this?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete",
        )
    }
}

@Composable
private fun MissingMediaState(
    state: MediaDownloadState,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TextDefault(
                text = when (state) {
                    MediaDownloadState.PENDING -> "This media is still syncing."
                    MediaDownloadState.STALE -> "Refreshing the local file."
                    MediaDownloadState.FAILED -> "The local download failed."
                    MediaDownloadState.READY -> "Media not available."
                },
            )
            if (state != MediaDownloadState.PENDING) {
                PrimaryButton(
                    text = "Retry download",
                    onClick = onRetry,
                    modifier = Modifier.padding(top = LifeTogetherTokens.spacing.small),
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
            text = media.dateCreated.toDateTimeString()
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
            uiState = MediaDetailsUiState.Content(
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
