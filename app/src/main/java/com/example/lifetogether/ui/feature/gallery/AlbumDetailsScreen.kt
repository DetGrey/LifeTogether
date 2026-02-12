package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.OverflowMenu
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.dialog.CustomConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.MediaUploadMultipleDialog
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.model.MenuAction
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.example.lifetogether.ui.viewmodel.ImageViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailsScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    albumId: String,
) {
    val albumDetailsViewModel: AlbumDetailsViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val uiState by albumDetailsViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = albumId) {
        userInformation?.familyId?.let {
            albumDetailsViewModel.setUpAlbumMedia(it, albumId)
        }
    }

    // Material3 pull-to-refresh state
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                onRefresh = { albumDetailsViewModel.retryFetchAlbumMedia() },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = { appNavigator?.navigateBack() },
                text = uiState.album?.itemName ?: "Album images",
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { albumDetailsViewModel.toggleOverflowMenu() },
            )
            when (uiState.isSelectionModeActive) {
                true -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompletableBox(
                                isCompleted = uiState.isAllMediaSelected,
                                onCompleteToggle = {
                                    albumDetailsViewModel.toggleAllMediaSelection()
                                }
                            )
                            TextDefault(text = "All")
                        }
                        val selectedMediaCount = uiState.selectedMedia.size
                        TextDefault(text = "$selectedMediaCount selected")
                        TextDefault(
                            text = "Cancel",
                            modifier = Modifier.clickable {
                                albumDetailsViewModel.toggleSelectionMode()
                            }
                        )
                    }
                }
                false -> Spacer(modifier = Modifier.height(30.dp))
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
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                }
                LazyVerticalGrid(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(uiState.media.size) { index ->
                        val media = uiState.media[index]
                        val thumbnail = uiState.thumbnails[media.id]

                        LaunchedEffect(media.id) {
                            media.id?.let { albumDetailsViewModel.fetchThumbnail(it) }
                        }

                        val isVideo = media is GalleryVideo
                        val duration = (media as? GalleryVideo)?.duration

                        ThumbnailContainer(
                            thumbnail = thumbnail,
                            isVideo = isVideo,
                            duration = duration,
                            onClick = {
                                if (uiState.isSelectionModeActive) {
                                    albumDetailsViewModel.toggleMediaSelection(media.id)
                                } else {
                                    appNavigator?.navigateToGalleryMedia(albumId, index)
                                }
                            },
                            onLongClick = {
                                if (!uiState.isSelectionModeActive) {
                                    albumDetailsViewModel.toggleSelectionMode()
                                    albumDetailsViewModel.toggleMediaSelection(media.id)
                                }
                            },
                            isSelectionMode = uiState.isSelectionModeActive,
                            isSelected = uiState.selectedMedia.contains(media.id),
                            onSelectionToggle = {
                                albumDetailsViewModel.toggleMediaSelection(media.id)
                            }
                        )
                    }
                }
            }
        }

        PullToRefreshDefaults.Indicator(
            state = pullToRefreshState,
            isRefreshing = uiState.isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }

    // ---------------------------------------------------------------- ADD NEW IMAGE
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 30.dp, end = 30.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        AddButton(onClick = { imageViewModel.showImageUploadDialog = true })
    }

    // ---------------------------------------------------------------- OVERFLOW MENU
    if (uiState.showOverflowMenu) {
        val actions = when (uiState.isSelectionModeActive) {
            true -> MenuAction.SelectionActions.entries
            false -> MenuAction.AlbumActions.entries
        }

        OverflowMenu(
            onDismiss = { albumDetailsViewModel.toggleOverflowMenu() },
            actionsList = actions.map {
                mapOf(it.label to { albumDetailsViewModel.startOverflowAction(it) })
            }
        )
    }

    // ---------------------------------------------------------------- IMAGE UPLOAD DIALOG
    if (imageViewModel.showImageUploadDialog && userInformation != null) {
        userInformation!!.familyId?.let { familyId ->
            MediaUploadMultipleDialog(
                onDismiss = { imageViewModel.showImageUploadDialog = false },
                onConfirm = { imageViewModel.showImageUploadDialog = false },
                dialogTitle = "Upload images",
                dialogMessage = "Select the images to upload",
                imageType = ImageType.GalleryMedia(familyId, albumId, null),
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload images",
            )
        }
    }

    // ---------------------------------------------------------------- OVERFLOW MENU ACTIONS DIALOG
    if (uiState.showOverflowMenuActionDialog && uiState.overflowMenuAction != null) {
        when (val action = uiState.overflowMenuAction) {
            is MenuAction.AlbumActions -> {
                when (action) {
                    MenuAction.AlbumActions.RENAME -> {
                        ConfirmationDialogWithTextField(
                            onDismiss = { albumDetailsViewModel.dismissOverflowMenuActionDialog() },
                            onConfirm = { albumDetailsViewModel.renameAlbum() },
                            dialogTitle = "Rename album",
                            dialogMessage = "Enter a new name for the album",
                            dismissButtonMessage = "Cancel",
                            confirmButtonMessage = "Rename album",
                            textValue = uiState.actionDialogText,
                            onTextValueChange = { albumDetailsViewModel.setActionDialogText(it) },
                            capitalization = true,
                        )
                    }
                    MenuAction.AlbumActions.DELETE -> {
                        ConfirmationDialog(
                            onDismiss = { albumDetailsViewModel.dismissOverflowMenuActionDialog() },
                            onConfirm = {
                                albumDetailsViewModel.deleteAlbum(onDeleteSuccess = { appNavigator?.navigateBack() })
                            },
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
                        albumDetailsViewModel.downloadSelectedMedia()
                    }
                    MenuAction.SelectionActions.DELETE -> {
                        ConfirmationDialog(
                            onDismiss = { albumDetailsViewModel.dismissOverflowMenuActionDialog() },
                            onConfirm = {
                                albumDetailsViewModel.deleteSelectedMedia()
                            },
                            dialogTitle = "Delete selected media",
                            dialogMessage = "Are you sure you want to delete the selected media?",
                            dismissButtonMessage = "Cancel",
                            confirmButtonMessage = "Delete selected",
                        )
                    }
                    MenuAction.SelectionActions.MOVE -> {
                        CustomConfirmationDialog(
                            onDismiss = { albumDetailsViewModel.dismissOverflowMenuActionDialog() },
                            onConfirm = {
                                albumDetailsViewModel.showError("Please choose an album first")
                            },
                            dialogTitle = "Move to another album",
                            dialogMessage = "Are you sure you want to move the selected media to another album?",
                            dismissButtonMessage = "Cancel",
                            confirmButtonMessage = "Move",
                            content = {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    maxItemsInEachRow = 2,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    for (album in uiState.albums) {
                                        AlbumContainer(
                                            album.name,
                                            album.mediaCount,
                                            album.thumbnail?.toBitmap(),
                                            onClick = {
                                                albumDetailsViewModel.moveSelectedMediaToAlbum(album.id)
                                            },
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
            else -> {}
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
    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (uiState.showAlertDialog) {
        ErrorAlertDialog(error = uiState.error)
        albumDetailsViewModel.dismissAlert()
    }
}
