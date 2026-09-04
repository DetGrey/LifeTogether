package com.example.lifetogether.ui.feature.admin.beachAlbums

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.beach.BeachAlbum
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.feature.gallery.AlbumCard
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminBeachAlbumsScreen(
    uiState: AdminBeachAlbumsUiState,
    onUiEvent: (AdminBeachAlbumsUiEvent) -> Unit,
    onNavigationEvent: (AdminBeachAlbumsNavigationEvent) -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                leftAppIcon = AppIcon(
                    resId = R.drawable.ic_back,
                    description = "back arrow icon",
                ),
                onLeftClick = { onNavigationEvent(AdminBeachAlbumsNavigationEvent.NavigateBack) },
                text = "Beach Albums",
            )
        },
        floatingActionButton = {
            if (uiState is AdminBeachAlbumsUiState.Content) {
                AddButton(onClick = {
                    onUiEvent(AdminBeachAlbumsUiEvent.OpenCreateDialog)
                })
            }
        },
    ) { padding ->
        AnimatedLoadingContent(
            isLoading = uiState is AdminBeachAlbumsUiState.Loading,
            label = "admin_beach_albums_loading",
            loadingContent = {
                Skeletons.GridCollection(modifier = Modifier.fillMaxSize())
            },
        ) {
            val contentState = uiState as? AdminBeachAlbumsUiState.Content ?: return@AnimatedLoadingContent

            PullToRefreshBox(
                isRefreshing = contentState.isRefreshing,
                onRefresh = { onUiEvent(AdminBeachAlbumsUiEvent.RefreshAlbums) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(LifeTogetherTokens.spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xLarge),
                ) {
                    item {
                        if (contentState.albums.isEmpty()) {
                            Text(
                                text = if (contentState.isSyncing) "Syncing beach albums…" else "No beach albums created yet. Press + to create one.",
                                modifier = Modifier.padding(top = LifeTogetherTokens.spacing.large),
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 2,
                                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                            ) {
                                for (album in contentState.albums) {
                                    AlbumCard(
                                        albumName = album.name,
                                        count = album.count,
                                        bitmap = contentState.thumbnails[album.id]?.toBitmap(),
                                        onClick = {
                                            onNavigationEvent(
                                                AdminBeachAlbumsNavigationEvent.NavigateToDetails(album.id)
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            when (val dialog = contentState.dialogState) {
                is BeachAlbumsDialogState.CreateAlbum -> {
                    ConfirmationDialogWithTextField(
                        onDismiss = { onUiEvent(AdminBeachAlbumsUiEvent.DismissDialog) },
                        onConfirm = { onUiEvent(AdminBeachAlbumsUiEvent.ConfirmCreateAlbum) },
                        dialogTitle = "Create Beach Album",
                        dialogMessage = "Enter a name for the new beach album:",
                        dismissButtonMessage = "Cancel",
                        confirmButtonMessage = "Create",
                        textValue = dialog.nameDraft,
                        onTextValueChange = { onUiEvent(AdminBeachAlbumsUiEvent.NameDraftChanged(it)) },
                        label = "Album Name",
                        capitalization = true,
                    )
                }
                null -> Unit
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminBeachAlbumsScreenPreview() {
    LifeTogetherTheme {
        AdminBeachAlbumsScreen(
            uiState = AdminBeachAlbumsUiState.Content(
                albums = listOf(
                    BeachAlbum(id = "1", familyId = "1", name = "Bondi Beach", count = 24),
                    BeachAlbum(id = "2", familyId = "1", name = "Malibu Beach", count = 10),
                )
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
