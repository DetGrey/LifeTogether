package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.sync.SyncUpdatingText
import com.example.lifetogether.domain.sync.SyncKey

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onUiEvent: (GalleryUiEvent) -> Unit,
    onNavigationEvent: (GalleryNavigationEvent) -> Unit,
) {
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
                    onLeftClick = { onNavigationEvent(GalleryNavigationEvent.NavigateBack) },
                    text = "Albums",
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SyncUpdatingText(
                        keys = setOf(SyncKey.GALLERY_ALBUMS, SyncKey.GALLERY_MEDIA),
                    )

                    if (uiState.albums.isEmpty()) {
                        Text(text = "No albums created. Press + to create one.")
                    } else {
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
                                        onNavigationEvent(GalleryNavigationEvent.NavigateToAlbumMedia(album.id))
                                    },
                                )
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
            onUiEvent(GalleryUiEvent.OpenNewAlbumDialog)
        })
    }

    if (uiState.showNewAlbumDialog) {
        ConfirmationDialogWithTextField(
            onDismiss = { onUiEvent(GalleryUiEvent.DismissNewAlbumDialog) },
            onConfirm = { onUiEvent(GalleryUiEvent.CreateNewAlbum) },
            dialogTitle = "Create new album",
            dialogMessage = "Please enter a name for your new album",
            dismissButtonMessage = "Cancel",
            confirmButtonMessage = "Create",
            textValue = uiState.newAlbumName,
            onTextValueChange = { onUiEvent(GalleryUiEvent.NewAlbumNameChanged(it)) },
            capitalization = true,
        )
    }
}
