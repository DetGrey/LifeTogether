package com.example.lifetogether.ui.feature.gallery

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
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
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailsScreen(
    uiState: AlbumDetailsUiState,
    onImageUpload: suspend (List<Uri>) -> Result<Unit, AppError>,
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
                        leftIcon = Icon(
                            resId = R.drawable.ic_back_arrow,
                            description = "back arrow icon",
                        ),
                        onLeftClick = { onNavigationEvent(AlbumDetailsNavigationEvent.NavigateBack) },
                        text = "Album images",
                    )
                },
            ) { padding ->
                Skeletons.GalleryGrid(modifier = Modifier.fillMaxSize().padding(padding))
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AlbumDetailsContent(
    uiState: AlbumDetailsUiState.Content,
    onImageUpload: suspend (List<Uri>) -> Result<Unit, AppError>,
    onUiEvent: (AlbumDetailsUiEvent) -> Unit,
    onNavigationEvent: (AlbumDetailsNavigationEvent) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            AppTopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(AlbumDetailsNavigationEvent.NavigateBack) },
                text = uiState.album?.itemName ?: "Album images",
                rightIcon = Icon(
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
                when (uiState.isSelectionModeActive) {
                    true -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CompletableBox(
                                    isCompleted = uiState.isAllMediaSelected,
                                    onCompleteToggle = {
                                        onUiEvent(AlbumDetailsUiEvent.ToggleAllMediaSelection)
                                    },
                                )
                                TextDefault(text = "All")
                            }
                            val selectedMediaCount = uiState.selectedMedia.size
                            TextDefault(text = "$selectedMediaCount selected")
                            TextDefault(
                                text = "Cancel",
                                modifier = Modifier.clickable {
                                    onUiEvent(AlbumDetailsUiEvent.ToggleSelectionMode)
                                },
                            )
                        }
                    }

                    false -> Spacer(modifier = Modifier.height(20.dp))
                }

                if (uiState.media.isEmpty()) {
                    if (uiState.isSyncing) {
                        TextDefault(text = "Syncing media…")
                    } else {
                        TextDefault(text = "No images in this album. Press + to create one.")
                    }
                } else {
                    if (uiState.isPartialLoad) {
                        TextDefault(
                            text = "⚠ Only ${uiState.media.size} of ${uiState.album?.count} items loaded. Pull to refresh to retry.",
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

                                ThumbnailContainer(
                                    thumbnail = thumbnail,
                                    isVideo = isVideo,
                                    duration = duration,
                                    onClick = {
                                        if (uiState.isSelectionModeActive) {
                                            onUiEvent(AlbumDetailsUiEvent.ToggleMediaSelection(media.id))
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
            true -> MenuAction.SelectionActions.entries
            false -> MenuAction.AlbumActions.entries
        }

        ActionSheet(
            onDismiss = { onUiEvent(AlbumDetailsUiEvent.ToggleOverflowMenu) },
            actionsList = actions.map {
                ActionSheetItem(
                    label = it.label,
                    onClick = { onUiEvent(AlbumDetailsUiEvent.StartOverflowAction(it)) },
                    isDestructive = when (it) {
                        MenuAction.AlbumActions.DELETE,
                        MenuAction.SelectionActions.DELETE -> true

                        else -> false
                    },
                )
            },
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

    if (uiState.showOverflowMenuActionDialog && uiState.overflowMenuAction != null) {
        when (val action = uiState.overflowMenuAction) {
            is MenuAction.AlbumActions -> {
                when (action) {
                    MenuAction.AlbumActions.RENAME -> {
                        ConfirmationDialogWithTextField(
                            onDismiss = { onUiEvent(AlbumDetailsUiEvent.DismissOverflowMenuActionDialog) },
                            onConfirm = { onUiEvent(AlbumDetailsUiEvent.ConfirmRenameAlbum) },
                            dialogTitle = "Rename album",
                            dialogMessage = "Enter a new name for the album",
                            dismissButtonMessage = "Cancel",
                            confirmButtonMessage = "Rename album",
                            textValue = uiState.actionDialogText,
                            onTextValueChange = { onUiEvent(AlbumDetailsUiEvent.SetActionDialogText(it)) },
                            label = "New album name",
                            capitalization = true,
                        )
                    }

                    MenuAction.AlbumActions.DELETE -> {
                        ConfirmationDialog(
                            onDismiss = { onUiEvent(AlbumDetailsUiEvent.DismissOverflowMenuActionDialog) },
                            onConfirm = { onUiEvent(AlbumDetailsUiEvent.ConfirmDeleteAlbum) },
                            dialogTitle = "Delete album",
                            dialogMessage = "Are you sure you want to delete this album?",
                            dismissButtonMessage = "Cancel",
                            confirmButtonMessage = "Delete album",
                        )
                    }
                }
            }

            is MenuAction.SelectionActions -> {
                when (action) {
                    MenuAction.SelectionActions.DOWNLOAD -> {
                        onUiEvent(AlbumDetailsUiEvent.DownloadSelectedMedia)
                    }

                    MenuAction.SelectionActions.DELETE -> {
                        ConfirmationDialog(
                            onDismiss = { onUiEvent(AlbumDetailsUiEvent.DismissOverflowMenuActionDialog) },
                            onConfirm = { onUiEvent(AlbumDetailsUiEvent.ConfirmDeleteSelectedMedia) },
                            dialogTitle = "Delete selected media",
                            dialogMessage = "Are you sure you want to delete the selected media?",
                            dismissButtonMessage = "Cancel",
                            confirmButtonMessage = "Delete selected",
                        )
                    }

                    MenuAction.SelectionActions.MOVE -> {
                        ConfirmationDialog(
                            onDismiss = { onUiEvent(AlbumDetailsUiEvent.DismissOverflowMenuActionDialog) },
                            onConfirm = { onUiEvent(AlbumDetailsUiEvent.ConfirmMoveSelectedMedia) },
                            dialogTitle = "Move to another album",
                            dialogMessage = "Are you sure you want to move the selected media to another album?",
                            dismissButtonMessage = "Cancel",
                            confirmButtonMessage = "Move",
                            content = {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    maxItemsInEachRow = 2,
                                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                                ) {
                                    for (album in uiState.albums) {
                                        AlbumCard(
                                            album.name,
                                            album.mediaCount,
                                            album.thumbnail?.toBitmap(),
                                            onClick = {
                                                onUiEvent(AlbumDetailsUiEvent.MoveSelectedMediaToAlbum(album.id))
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }

            else -> Unit
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumDetailsScreenLoadingPreview() {
    LifeTogetherTheme {
        AlbumDetailsScreen(
            uiState = AlbumDetailsUiState.Loading,
            onImageUpload = { Result.Success(Unit) },
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
                overflowMenuAction = null,
                actionDialogText = "",
                selectedMedia = emptySet(),
                albums = emptyList(),
                familyId = "family-1",
            ),
            onImageUpload = { Result.Success(Unit) },
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
