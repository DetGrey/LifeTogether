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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.gallery.Album
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumMediaScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    albumId: String,
) {
    val albumMediaViewModel: AlbumMediaViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val album by albumMediaViewModel.album.collectAsState()
    val albumMedia by albumMediaViewModel.albumMedia.collectAsState()
    val thumbnails by albumMediaViewModel.thumbnails.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        userInformation?.familyId?.let {
            albumMediaViewModel.setUpAlbumMedia(it, albumId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
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
                text = when (album) {
                    is Album -> {
                        album!!.itemName
                    }
                    else -> "Album images"
                },
                rightIcon = Icon(
                    resId = R.drawable.ic_overflow_menu,
                    description = "overflow menu",
                ),
                onRightClick = {
                    albumMediaViewModel.toggleOverflowMenu()
                },

            )

            if (albumMedia.isEmpty()) {
                Text(text = "No images in this album. Press + to create one.")
            } else {
                LazyVerticalGrid(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth(),
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(albumMedia.size) { index ->
                        val media = albumMedia[index]
                        val thumbnail = thumbnails[media.id]

                        LaunchedEffect(media.id) {
                            media.id?.let { albumMediaViewModel.fetchThumbnail(it) }
                        }

                        if (media is GalleryImage) {
                            ThumbnailContainer(
                                thumbnail = thumbnail,
                                onClick = {
                                    media.id?.let {
                                        appNavigator?.navigateToGalleryMedia(media.id!!)
                                    }
                                },
                            )
                        } else if (media is GalleryVideo) {
                            ThumbnailContainer(
                                thumbnail = thumbnail,
                                onClick = {
                                    media.id?.let {
                                        appNavigator?.navigateToGalleryMedia(media.id!!)
                                    }
                                },
                                isVideo = true,
                                duration = media.duration,
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- ADD NEW IMAGE
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 30.dp, end = 30.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        AddButton(onClick = {
            imageViewModel.showImageUploadDialog = true
        })
    }

    // ---------------------------------------------------------------- OVERFLOW MENU
    if (albumMediaViewModel.showOverflowMenu) {
        OverflowMenu(
            onDismiss = { albumMediaViewModel.toggleOverflowMenu() },
            actionsList = listOf(
                mapOf(
                    "Rename album" to {
                        albumMediaViewModel.overflowMenuAction = AlbumMediaViewModel.OverflowMenuActions.RENAME_ALBUM
                        albumMediaViewModel.showOverflowMenuActionDialog = true
                    },
                ),
                mapOf(
                    "Select media" to {
                        // todo
                    },
                ),
                mapOf(
                    "Delete album" to {
                        albumMediaViewModel.overflowMenuAction = AlbumMediaViewModel.OverflowMenuActions.DELETE_ALBUM
                        albumMediaViewModel.showOverflowMenuActionDialog = true
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
    if (albumMediaViewModel.showOverflowMenuActionDialog && albumMediaViewModel.overflowMenuAction != null) {
        when (albumMediaViewModel.overflowMenuAction) {
            AlbumMediaViewModel.OverflowMenuActions.RENAME_ALBUM -> {
                ConfirmationDialogWithTextField(
                    onDismiss = {
                        albumMediaViewModel.dismissOverflowMenuActionDialog()
                    },
                    onConfirm = {
                        albumMediaViewModel.renameAlbum()
                    },
                    dialogTitle = "Rename album",
                    dialogMessage = "Enter a new name for the album",
                    dismissButtonMessage = "Cancel",
                    confirmButtonMessage = "Rename album",
                    textValue = albumMediaViewModel.actionDialogText,
                    onTextValueChange = { albumMediaViewModel.actionDialogText = it },
                    capitalization = true,
                )
            }
            AlbumMediaViewModel.OverflowMenuActions.SELECT_MEDIA -> {
            }
            AlbumMediaViewModel.OverflowMenuActions.DELETE_ALBUM -> {
                ConfirmationDialog(
                    onDismiss = {
                        albumMediaViewModel.dismissOverflowMenuActionDialog()
                    },
                    onConfirm = {
                        albumMediaViewModel.deleteAlbum(onDeleteSuccess = {
                            appNavigator?.navigateBack()
                        })
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
    if (albumMediaViewModel.showAlertDialog) {
        ErrorAlertDialog(albumMediaViewModel.error)
        albumMediaViewModel.toggleAlertDialog()
    }
}
