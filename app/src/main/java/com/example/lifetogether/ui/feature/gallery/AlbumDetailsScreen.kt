package com.example.lifetogether.ui.feature.gallery

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.image.MediaUploadMultipleDialog
import com.example.lifetogether.ui.common.list.SelectionModeBar
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlbumDetailsScreen(
    uiState: AlbumDetailsUiState,
    onImageUpload: suspend (List<Uri>, (current: Int, total: Int) -> Unit) -> Result<Unit, AppError>,
    onUiEvent: (AlbumDetailsUiEvent) -> Unit,
    onNavigationEvent: (AlbumDetailsNavigationEvent) -> Unit,
) {
    AnimatedLoadingContent(
        isLoading = uiState is AlbumDetailsUiState.Loading,
        label = "album_details_loading_content",
        loadingContent = {
            Scaffold(
                topBar = {
                    AppTopBar(
                        leftAppIcon = AppIcon(
                            resId = R.drawable.ic_back_arrow,
                            description = "back arrow icon",
                        ),
                        onLeftClick = { onNavigationEvent(AlbumDetailsNavigationEvent.NavigateBack) },
                        text = "Album images",
                    )
                },
            ) { padding ->
                Skeletons.GalleryGrid(modifier = Modifier
                    .fillMaxSize()
                    .padding(padding))
            }
        },
    ) {
        val content = uiState as? AlbumDetailsUiState.Content ?: return@AnimatedLoadingContent
        AlbumDetailsContent(
            uiState = content,
            onImageUpload = onImageUpload,
            onUiEvent = onUiEvent,
            onNavigationEvent = onNavigationEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailsContent(
    uiState: AlbumDetailsUiState.Content,
    onImageUpload: suspend (List<Uri>, (current: Int, total: Int) -> Unit) -> Result<Unit, AppError>,
    onUiEvent: (AlbumDetailsUiEvent) -> Unit,
    onNavigationEvent: (AlbumDetailsNavigationEvent) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    var showDeleteAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteSelectedMediaDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = uiState.isSelectionModeActive) {
        onUiEvent(AlbumDetailsUiEvent.ToggleSelectionMode)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    if (uiState.isSelectionModeActive) {
                        onUiEvent(AlbumDetailsUiEvent.ToggleSelectionMode)
                    } else {
                        onNavigationEvent(AlbumDetailsNavigationEvent.NavigateBack)
                    }
                },
                text = uiState.album?.itemName ?: "Album images",
                rightAppIcon = AppIcon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { onUiEvent(AlbumDetailsUiEvent.ToggleOverflowMenu) },
            )
        },
        floatingActionButton = {
            AddButton(onClick = { onUiEvent(AlbumDetailsUiEvent.RequestImageUpload) })
        },
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = pullToRefreshState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = { onUiEvent(AlbumDetailsUiEvent.RetryFetchAlbumMedia) },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LifeTogetherTokens.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            ) {
                if (uiState.isSelectionModeActive) {
                    SelectionModeBar(
                        selectedCount = uiState.selectedMedia.size,
                        isAllSelected = uiState.isAllMediaSelected,
                        onToggleAll = { onUiEvent(AlbumDetailsUiEvent.ToggleAllMediaSelection) },
                        onCancel = { onUiEvent(AlbumDetailsUiEvent.ToggleSelectionMode) },
                    )
                }

                if (uiState.media.isEmpty()) {
                    if (uiState.isSyncing) {
                        TextDefault(text = "Syncing media…")
                    } else {
                        TextDefault(text = "No images in this album. Press + to create one.")
                    }
                } else {
                    val failedCount = uiState.media.count { it.downloadState == MediaDownloadState.FAILED }
                    val syncingCount = uiState.media.count {
                        it.downloadState == MediaDownloadState.PENDING || it.downloadState == MediaDownloadState.STALE
                    }
                    if (uiState.isPartialLoad) {
                        TextDefault(
                            text = when {
                                failedCount > 0 -> "$failedCount media item(s) failed to download. Tap a failed tile or pull to retry."
                                syncingCount > 0 -> "$syncingCount media item(s) are still syncing."
                                else -> "⚠ Only ${uiState.media.size} of ${uiState.album?.count} items loaded. Pull to refresh to retry."
                            },
                            modifier = Modifier.padding(vertical = LifeTogetherTokens.spacing.small),
                        )
                    }
                    LazyVerticalGrid(
                        modifier = Modifier
                            .padding(LifeTogetherTokens.spacing.small)
                            .fillMaxWidth(),
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                        horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                    ) {
                        uiState.groupedMedia.forEach { (dateKey, itemsInDay) ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                TextSubHeadingMedium(dateKey)
                            }

                            items(itemsInDay) { media ->
                                val thumbnail = uiState.thumbnails[media.id]
                                val isVideo = media is GalleryVideo
                                val duration = (media as? GalleryVideo)?.duration
                                val globalIndex = uiState.media.indexOf(media)
                                val shouldRetryOnClick = media.mediaUri == null
                                        && media.downloadState != MediaDownloadState.READY
                                val statusLabel = when (media.downloadState) {
                                    MediaDownloadState.FAILED -> {
                                        if (uiState.retryingMediaIds.contains(media.id)) "Retrying…"
                                        else "Tap to retry"
                                    }
                                    MediaDownloadState.PENDING -> "Syncing…"
                                    MediaDownloadState.STALE ->
                                        if (thumbnail == null) "Refreshing…" else null
                                    MediaDownloadState.READY -> null
                                }

                                ThumbnailContainer(
                                    thumbnail = thumbnail,
                                    isVideo = isVideo,
                                    duration = duration,
                                    onClick = {
                                        if (uiState.isSelectionModeActive) {
                                            onUiEvent(AlbumDetailsUiEvent.ToggleMediaSelection(media.id))
                                        } else if (shouldRetryOnClick) {
                                            onUiEvent(AlbumDetailsUiEvent.RetryMediaDownload(media.id))
                                        } else {
                                            onNavigationEvent(
                                                AlbumDetailsNavigationEvent.NavigateToMediaDetails(globalIndex),
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        if (!uiState.isSelectionModeActive) {
                                            onUiEvent(AlbumDetailsUiEvent.EnterSelectionMode(media.id))
                                        }
                                    },
                                    isSelectionMode = uiState.isSelectionModeActive,
                                    isSelected = uiState.selectedMedia.contains(media.id),
                                    onSelectionToggle = {
                                        onUiEvent(AlbumDetailsUiEvent.ToggleMediaSelection(media.id))
                                    },
                                    statusLabel = statusLabel,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showOverflowMenu) {
        val actions = when (uiState.isSelectionModeActive) {
            true -> MenuAction.SelectionActions.entries.map { action ->
                ActionSheetItem(
                    label = action.label,
                    onClick = {
                        when (action) {
                            MenuAction.SelectionActions.DOWNLOAD -> {
                                onUiEvent(AlbumDetailsUiEvent.ToggleOverflowMenu)
                                onUiEvent(AlbumDetailsUiEvent.DownloadSelectedMedia)
                            }
                            MenuAction.SelectionActions.DELETE -> {
                                onUiEvent(AlbumDetailsUiEvent.ToggleOverflowMenu)
                                showDeleteSelectedMediaDialog = true
                            }
                            MenuAction.SelectionActions.MOVE -> {
                                onUiEvent(AlbumDetailsUiEvent.RequestMoveSelectedMedia)
                            }
                        }
                    },
                    isDestructive = action == MenuAction.SelectionActions.DELETE,
                )
            }
            false -> MenuAction.AlbumActions.entries.map { action ->
                ActionSheetItem(
                    label = action.label,
                    onClick = {
                        when (action) {
                            MenuAction.AlbumActions.RENAME -> {
                                onUiEvent(AlbumDetailsUiEvent.RequestRenameAlbum)
                            }
                            MenuAction.AlbumActions.DELETE -> {
                                onUiEvent(AlbumDetailsUiEvent.ToggleOverflowMenu)
                                showDeleteAlbumDialog = true
                            }
                        }
                    },
                    isDestructive = action == MenuAction.AlbumActions.DELETE,
                )
            }
        }

        ActionSheet(
            onDismiss = { onUiEvent(AlbumDetailsUiEvent.ToggleOverflowMenu) },
            actionsList = actions,
        )
    }

    if (uiState.showImageUploadDialog) {
        if (uiState.familyId != null && uiState.album?.id != null) {
            MediaUploadMultipleDialog(
                onDismiss = { onUiEvent(AlbumDetailsUiEvent.DismissImageUploadDialog) },
                onConfirm = { onUiEvent(AlbumDetailsUiEvent.ConfirmImageUploadDialog) },
                onUpload = onImageUpload,
                dialogTitle = "Upload images",
                dialogMessage = "Select the images to upload",
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload images",
            )
        }
    }

    when (val dialog = uiState.dialog) {
        is AlbumDetailsDialogState.RenameAlbum -> ConfirmationDialogWithTextField(
            onDismiss = { onUiEvent(AlbumDetailsUiEvent.DismissDialog) },
            onConfirm = { onUiEvent(AlbumDetailsUiEvent.ConfirmRenameAlbum) },
            dialogTitle = "Rename album",
            dialogMessage = "Enter a new name for the album",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Rename album",
            textValue = dialog.name,
            onTextValueChange = { onUiEvent(AlbumDetailsUiEvent.RenameAlbumNameChanged(it)) },
            label = "New album name",
            capitalization = true,
        )

        is AlbumDetailsDialogState.MoveSelectedMedia -> ConfirmationDialog(
            onDismiss = { onUiEvent(AlbumDetailsUiEvent.DismissDialog) },
            onConfirm = { onUiEvent(AlbumDetailsUiEvent.ConfirmMoveSelectedMedia) },
            dialogTitle = "Move to another album",
            dialogMessage = "Choose a target album, then tap Move.",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Move",
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = 2,
                    ) {
                        uiState.albums.forEach { album ->
                            AlbumCard(
                                album.name,
                                album.mediaCount,
                                album.thumbnail?.toBitmap(),
                                isSelected = dialog.targetAlbumId == album.id,
                                onClick = {
                                    onUiEvent(AlbumDetailsUiEvent.MoveSelectedMediaToAlbum(album.id))
                                },
                            )
                        }
                    }
                }
            },
        )

        null -> Unit
    }

    if (showDeleteAlbumDialog) {
        ConfirmationDialog(
            onDismiss = { showDeleteAlbumDialog = false },
            onConfirm = {
                showDeleteAlbumDialog = false
                onUiEvent(AlbumDetailsUiEvent.ConfirmDeleteAlbum)
            },
            dialogTitle = "Delete album",
            dialogMessage = "Are you sure you want to delete this album?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete album",
        )
    }

    if (showDeleteSelectedMediaDialog) {
        ConfirmationDialog(
            onDismiss = { showDeleteSelectedMediaDialog = false },
            onConfirm = {
                showDeleteSelectedMediaDialog = false
                onUiEvent(AlbumDetailsUiEvent.ConfirmDeleteSelectedMedia)
            },
            dialogTitle = "Delete selected media",
            dialogMessage = "Are you sure you want to delete the selected media?",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Delete selected",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumDetailsScreenLoadingPreview() {
    LifeTogetherTheme {
        AlbumDetailsScreen(
            uiState = AlbumDetailsUiState.Loading,
            onImageUpload = { _, _ -> Result.Success(Unit) },
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumDetailsScreenPreview() {
    LifeTogetherTheme {
        val sampleMedia = GalleryImage(
            id = "media-1",
            familyId = "family-1",
            itemName = "Kitchen shelf",
            lastUpdated = java.util.Date(1_717_200_000_000),
            albumId = "album-1",
            dateCreated = java.util.Date(1_717_200_000_000),
        )

        AlbumDetailsScreen(
            uiState = AlbumDetailsUiState.Content(
                album = Album(
                    id = "album-1",
                    familyId = "family-1",
                    itemName = "Weekend trip",
                    lastUpdated = java.util.Date(1_717_200_000_000),
                    count = 1,
                ),
                media = listOf(sampleMedia),
                groupedMedia = listOf("Today" to listOf(sampleMedia)),
                thumbnails = emptyMap(),
                selectedMedia = emptySet(),
                albums = emptyList(),
                familyId = "family-1",
            ),
            onImageUpload = { _, _ -> Result.Success(Unit) },
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
