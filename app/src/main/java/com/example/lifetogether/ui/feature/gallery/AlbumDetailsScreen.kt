package com.example.lifetogether.ui.feature.gallery

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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.OverflowMenu
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.image.MediaUploadMultipleDialog
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.domain.sync.SyncKey
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailsScreen(
    uiState: AlbumDetailsUiState,
    onImageUpload: suspend (List<Uri>) -> Result<Unit, AppError>,
    onUiEvent: (AlbumDetailsUiEvent) -> Unit,
    onNavigationEvent: (AlbumDetailsNavigationEvent) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopBar(
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
                SyncUpdatingText(
                    keys = setOf(SyncKey.GALLERY_ALBUMS, SyncKey.GALLERY_MEDIA),
                )
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

        OverflowMenu(
            onDismiss = { onUiEvent(AlbumDetailsUiEvent.ToggleOverflowMenu) },
            actionsList = actions.map {
                mapOf(it.label to { onUiEvent(AlbumDetailsUiEvent.StartOverflowAction(it)) })
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
                                        AlbumContainer(
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
