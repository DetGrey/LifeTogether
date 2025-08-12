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
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.ImageUploadMultipleDialog
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.example.lifetogether.ui.viewmodel.ImageViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlbumImagesScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    albumId: String,
) {
    val albumImagesViewModel: AlbumImagesViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val album by albumImagesViewModel.album.collectAsState()
    val albumImages by albumImagesViewModel.albumImages.collectAsState()
    val thumbnails by albumImagesViewModel.thumbnails.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        userInformation?.familyId?.let {
            albumImagesViewModel.setUpAlbumImages(it, albumId)
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
            )

            if (albumImages.isEmpty()) {
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
                    items(albumImages.size) { index ->
                        val image = albumImages[index]
                        val thumbnail = thumbnails[image.id]

                        LaunchedEffect(image.id) {
                            image.id?.let { albumImagesViewModel.fetchThumbnail(it) }
                        }

                        ImageContainer(
                            thumbnail = thumbnail,
                            onClick = {
                                image.id?.let {
                                    appNavigator?.navigateToGalleryImage(image.id!!)
                                }
                            },
                        )
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

    // ---------------------------------------------------------------- IMAGE UPLOAD DIALOG
    if (imageViewModel.showImageUploadDialog && userInformation != null) {
        userInformation!!.familyId?.let { familyId ->
            ImageUploadMultipleDialog(
                onDismiss = { imageViewModel.showImageUploadDialog = false },
                onConfirm = { imageViewModel.showImageUploadDialog = false },
                dialogTitle = "Upload images",
                dialogMessage = "Select the images to upload",
                imageType = ImageType.GalleryImage(familyId, albumId, listOf()),
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload images",
            )
        }
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (albumImagesViewModel.showAlertDialog) {
        ErrorAlertDialog(albumImagesViewModel.error)
        albumImagesViewModel.toggleAlertDialog()
    }
}
