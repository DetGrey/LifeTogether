package com.example.lifetogether.ui.feature.gallery

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.common.image.ImageUploadDialog
import com.example.lifetogether.ui.common.image.ImageUploadMultipleDialog
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel
import com.example.lifetogether.ui.viewmodel.GalleryViewModel
import com.example.lifetogether.ui.viewmodel.GalleryViewModel.GalleryType
import com.example.lifetogether.ui.viewmodel.ImageViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
) {
    val galleryViewModel: GalleryViewModel = hiltViewModel()
    val imageViewModel: ImageViewModel = hiltViewModel()

    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val albums by galleryViewModel.albums.collectAsState()
    val galleryType by galleryViewModel.galleryType.collectAsState()
    val albumImages by galleryViewModel.selectedAlbumImages.collectAsState()

    LaunchedEffect(key1 = true) {
        // Perform any one-time initialization or side effect here
        println("Gallery familyId: ${userInformation?.familyId}")
        userInformation?.familyId?.let { galleryViewModel.setUpGallery(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                TopBar(
                    leftIcon = Icon(
                        resId = R.drawable.ic_back_arrow,
                        description = "back arrow icon",
                    ),
                    onLeftClick = {
                        when (galleryType) {
                            is GalleryType.Albums -> {
                                appNavigator?.navigateBack()
                            }
                            is GalleryType.Images -> {
                                galleryViewModel.toggleGalleryType()
                            }
                        }
                    },
                    text = when (galleryType) {
                        is GalleryType.Albums -> "Albums"
                        is GalleryType.Images -> (galleryType as GalleryType.Images).albumName
                    },
                )
            }

            item {
                when (galleryType) {
                    is GalleryType.Albums -> {
                        if (albums.isEmpty()) {
                            Text(text = "No albums created. Press + to create one.")
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 2,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                for (album in albums) {
                                    AlbumContainer(album.itemName, album.count, onClick = {
                                        galleryViewModel.toggleGalleryType(album.id, album.itemName)
                                    })
                                }
                            }
                        }
                    }
                    is GalleryType.Images -> {
                        if (albumImages.isEmpty()) {
                            Text(text = "No images in this album. Press + to create one.")
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 2,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                for (image in albumImages) {
//                                    ImageContainer(image.imageUrl)
                                }
                            }

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
            when (galleryType) {
                is GalleryType.Albums -> {
                    galleryViewModel.showNewAlbumDialog = true
                }

                is GalleryType.Images -> {
                    imageViewModel.showImageUploadDialog = true
                }
            }
        })
    }

    if (galleryViewModel.showNewAlbumDialog) {
        ConfirmationDialogWithTextField(
            onDismiss = { galleryViewModel.closeNewAlbumDialog() },
            onConfirm = {
                galleryViewModel.createNewAlbum()
            },
            dialogTitle = "Create new album",
            dialogMessage = "Please enter a name for your new album",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Create",
            textValue = galleryViewModel.newAlbumName,
            onTextValueChange = { galleryViewModel.newAlbumName = it },
            capitalization = true,
        )
    }

    // ---------------------------------------------------------------- IMAGE UPLOAD DIALOG
    if (imageViewModel.showImageUploadDialog && userInformation != null) {
        userInformation!!.familyId?.let { familyId ->
            ImageUploadMultipleDialog(
                onDismiss = { imageViewModel.showImageUploadDialog = false },
                onConfirm = { imageViewModel.showImageUploadDialog = false },
                dialogTitle = "Upload images",
                dialogMessage = "Select the images to upload",
                imageType = ImageType.GalleryImage(familyId, (galleryType as GalleryType.Images).albumId, listOf()),
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload images",
            )
        }
    }

    // ---------------------------------------------------------------- SHOW ERROR ALERT
    if (galleryViewModel.showAlertDialog) {
        ErrorAlertDialog(galleryViewModel.error)
        galleryViewModel.toggleAlertDialog()
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun GalleryScreenPreview() {
    LifeTogetherTheme {
        GalleryScreen()
    }
}
