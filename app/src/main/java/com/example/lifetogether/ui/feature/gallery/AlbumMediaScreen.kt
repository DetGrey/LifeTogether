package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.OverflowMenu
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.MediaUploadMultipleDialog
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.example.lifetogether.ui.viewmodel.ImageViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AlbumMediaScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    albumId: String,
) {
    val albumMediaViewModel: AlbumMediaViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val uiState by albumMediaViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = albumId) {
        userInformation?.familyId?.let {
            albumMediaViewModel.setUpAlbumMedia(it, albumId)
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
                onRefresh = { albumMediaViewModel.retryFetchAlbumMedia() },
            ),
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
                onLeftClick = { appNavigator?.navigateBack() },
                text = uiState.album?.itemName ?: "Album images",
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = { albumMediaViewModel.toggleOverflowMenu() },
            )

            if (uiState.media.isEmpty()) {
                if (uiState.isSyncing) {
                    Text(text = "Syncing media…")
                } else {
                    Text(text = "No images in this album. Press + to create one.")
                }
            } else {
                if (uiState.isPartialLoad) {
                    Text(
                        text = "⚠ Only ${uiState.media.size} of ${uiState.album?.count} items loaded. Pull to refresh to retry.",
                        modifier = Modifier.padding(vertical = 10.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
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
                            media.id?.let { albumMediaViewModel.fetchThumbnail(it) }
                        }

                        if (media is GalleryImage) {
                            ThumbnailContainer(
                                thumbnail = thumbnail,
                                onClick = { appNavigator?.navigateToGalleryMedia(albumId, index) },
                            )
                        } else if (media is GalleryVideo) {
                            ThumbnailContainer(
                                thumbnail = thumbnail,
                                onClick = { appNavigator?.navigateToGalleryMedia(albumId, index) },
                                isVideo = true,
                                duration = media.duration,
                            )
                        }
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
        OverflowMenu(
            onDismiss = { albumMediaViewModel.toggleOverflowMenu() },
            actionsList = listOf(
                mapOf(
                    "Rename album" to {
                        albumMediaViewModel.startOverflowAction(AlbumMediaViewModel.OverflowMenuActions.RENAME_ALBUM)
                    },
                ),
                mapOf(
                    "Select media" to {
                        // todo
                    },
                ),
                mapOf(
                    "Delete album" to {
                        albumMediaViewModel.startOverflowAction(AlbumMediaViewModel.OverflowMenuActions.DELETE_ALBUM)
                    },
                ),

            ),
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
        when (uiState.overflowMenuAction) {
            AlbumMediaViewModel.OverflowMenuActions.RENAME_ALBUM -> {
                ConfirmationDialogWithTextField(
                    onDismiss = { albumMediaViewModel.dismissOverflowMenuActionDialog() },
                    onConfirm = { albumMediaViewModel.renameAlbum() },
                    dialogTitle = "Rename album",
                    dialogMessage = "Enter a new name for the album",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Rename album",
                    textValue = uiState.actionDialogText,
                    onTextValueChange = { albumMediaViewModel.setActionDialogText(it) },
                    capitalization = true,
                )
            }
            AlbumMediaViewModel.OverflowMenuActions.SELECT_MEDIA -> {
            }
            AlbumMediaViewModel.OverflowMenuActions.DELETE_ALBUM -> {
                ConfirmationDialog(
                    onDismiss = { albumMediaViewModel.dismissOverflowMenuActionDialog() },
                    onConfirm = {
                        albumMediaViewModel.deleteAlbum(onDeleteSuccess = { appNavigator?.navigateBack() })
                    },
                    dialogTitle = "Delete album",
                    dialogMessage = "Are you sure you want to delete this album?",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Delete album",
                )
            }
            else -> {}
        }
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (uiState.showAlertDialog) {
        ErrorAlertDialog(uiState.error)
        albumMediaViewModel.dismissAlert()
    }
}
