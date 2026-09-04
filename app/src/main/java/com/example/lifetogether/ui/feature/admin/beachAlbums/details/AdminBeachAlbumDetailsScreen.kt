package com.example.lifetogether.ui.feature.admin.beachAlbums.details

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.data.logic.QrCodeGenerator
import com.example.lifetogether.domain.model.AppIcon
import com.example.lifetogether.domain.model.beach.BeachAlbum
import com.example.lifetogether.domain.model.beach.BeachMedia
import com.example.lifetogether.domain.model.gallery.MediaDownloadState
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.ui.common.ActionSheet
import com.example.lifetogether.ui.common.ActionSheetItem
import com.example.lifetogether.ui.common.AppTopBar
import com.example.lifetogether.ui.common.animation.AnimatedLoadingContent
import com.example.lifetogether.ui.common.button.AddButton
import com.example.lifetogether.ui.common.dialog.ConfirmationDialog
import com.example.lifetogether.ui.common.dialog.ConfirmationDialogWithTextField
import com.example.lifetogether.ui.common.list.SelectionModeBar
import com.example.lifetogether.ui.common.skeleton.Skeletons
import com.example.lifetogether.ui.feature.admin.beachAlbums.components.QrCodeDialog
import com.example.lifetogether.ui.feature.gallery.ThumbnailContainer
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AdminBeachAlbumDetailsScreen(
    uiState: AdminBeachAlbumDetailsUiState,
    onUiEvent: (AdminBeachAlbumDetailsUiEvent) -> Unit,
    onNavigationEvent: (AdminBeachAlbumDetailsNavigationEvent) -> Unit,
) {
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onUiEvent(AdminBeachAlbumDetailsUiEvent.UploadPhotos(uris))
        }
    }

    AnimatedLoadingContent(
        isLoading = uiState is AdminBeachAlbumDetailsUiState.Loading,
        label = "admin_beach_album_details_loading",
        loadingContent = {
            Scaffold(
                topBar = {
                    AppTopBar(
                        leftAppIcon = AppIcon(resId = R.drawable.ic_back, description = "back icon"),
                        onLeftClick = { onNavigationEvent(AdminBeachAlbumDetailsNavigationEvent.NavigateBack) },
                        text = "Beach Album",
                    )
                },
            ) { padding ->
                Skeletons.GalleryGrid(modifier = Modifier.fillMaxSize().padding(padding))
            }
        },
    ) {
        val content = uiState as? AdminBeachAlbumDetailsUiState.Content ?: return@AnimatedLoadingContent
        val pullToRefreshState = rememberPullToRefreshState()

        BackHandler(enabled = content.isSelectionMode) {
            onUiEvent(AdminBeachAlbumDetailsUiEvent.ToggleSelectionMode)
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    leftAppIcon = AppIcon(resId = R.drawable.ic_back, description = "back icon"),
                    onLeftClick = {
                        if (content.isSelectionMode) {
                            onUiEvent(AdminBeachAlbumDetailsUiEvent.ToggleSelectionMode)
                        } else {
                            onNavigationEvent(AdminBeachAlbumDetailsNavigationEvent.NavigateBack)
                        }
                    },
                    text = content.album.name,
                    rightAppIcon = AppIcon(resId = R.drawable.ic_overflow_menu, description = "album options"),
                    onRightClick = { onUiEvent(AdminBeachAlbumDetailsUiEvent.ToggleMoreActions) },
                )
            },
            floatingActionButton = {
                if (content.isSelectionMode && content.selectedMediaIds.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { onUiEvent(AdminBeachAlbumDetailsUiEvent.OpenDeleteMediaDialog) },
                        modifier = Modifier.size(60.dp),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        shape = CircleShape,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Delete selected photos",
                        )
                    }
                } else if (!content.isSelectionMode) {
                    AddButton(onClick = {
                        imagePickerLauncher.launch("image/*")
                    })
                }
            },
        ) { padding ->
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                state = pullToRefreshState,
                isRefreshing = content.isRefreshing,
                onRefresh = { onUiEvent(AdminBeachAlbumDetailsUiEvent.RetryFetchBeachMedia) },
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = content.isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (content.isSelectionMode) {
                        SelectionModeBar(
                            selectedCount = content.selectedMediaIds.size,
                            isAllSelected = content.mediaList.isNotEmpty() && content.selectedMediaIds.size == content.mediaList.size,
                            onToggleAll = { onUiEvent(AdminBeachAlbumDetailsUiEvent.ToggleSelectAll) },
                            onCancel = { onUiEvent(AdminBeachAlbumDetailsUiEvent.ToggleSelectionMode) },
                        )
                    }

                    if (content.mediaList.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(LifeTogetherTokens.spacing.large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (content.album.count > 0 || content.isSyncing) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Syncing media…",
                                    modifier = Modifier.padding(top = LifeTogetherTokens.spacing.medium),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = "No photos in this beach album yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = "Tap + to upload photos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    } else {
                        val failedCount = content.mediaList.count { it.downloadState == MediaDownloadState.FAILED }
                        val syncingCount = content.mediaList.count {
                            it.downloadState == MediaDownloadState.PENDING || it.downloadState == MediaDownloadState.STALE
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(LifeTogetherTokens.spacing.small),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                        ) {
                            if (content.isPartialLoad) {
                                Text(
                                    text = when {
                                        failedCount > 0 -> "$failedCount photo(s) failed to download. Tap a photo or pull to retry."
                                        syncingCount > 0 -> "$syncingCount photo(s) are still syncing."
                                        else -> "⚠ Only ${content.mediaList.size} of ${content.album.count} items loaded. Pull to refresh to retry."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = LifeTogetherTokens.spacing.small),
                                )
                            }
                            LazyVerticalGrid(
                                modifier = Modifier.fillMaxSize(),
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                            ) {
                                items(content.mediaList, key = { it.id }) { media ->
                                    val isSelected = content.selectedMediaIds.contains(media.id)
                                    val isRetrying = content.retryingMediaIds.contains(media.id)
                                    val statusLabel = when {
                                        isRetrying -> "Syncing…"
                                        media.downloadState == MediaDownloadState.PENDING ||
                                            media.downloadState == MediaDownloadState.STALE -> "Syncing…"
                                        media.downloadState == MediaDownloadState.FAILED -> "Tap to retry"
                                        else -> null
                                    }
                                    ThumbnailContainer(
                                        thumbnail = media.thumbnail,
                                        onClick = { onUiEvent(AdminBeachAlbumDetailsUiEvent.MediaClicked(media.id)) },
                                        onLongClick = { onUiEvent(AdminBeachAlbumDetailsUiEvent.MediaLongClicked(media.id)) },
                                        isSelectionMode = content.isSelectionMode,
                                        isSelected = isSelected,
                                        onSelectionToggle = { onUiEvent(AdminBeachAlbumDetailsUiEvent.MediaClicked(media.id)) },
                                        statusLabel = statusLabel,
                                    )
                                }
                            }
                        }
                    }

                    content.uploadProgress?.let { progress ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Text(
                                    text = "Uploading ${progress.current} of ${progress.total}...",
                                    modifier = Modifier.padding(top = LifeTogetherTokens.spacing.medium),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            if (content.showMoreActions) {
                val actions = if (content.isSelectionMode) {
                    listOf(
                        ActionSheetItem(
                            label = "Delete photo(s)",
                            onClick = {
                                onUiEvent(AdminBeachAlbumDetailsUiEvent.ToggleMoreActions)
                                if (content.selectedMediaIds.isNotEmpty()) {
                                    onUiEvent(AdminBeachAlbumDetailsUiEvent.OpenDeleteMediaDialog)
                                }
                            },
                            isDestructive = true,
                        ),
                    )
                } else {
                    listOf(
                        ActionSheetItem(
                            label = "Get QR code",
                            onClick = { onUiEvent(AdminBeachAlbumDetailsUiEvent.OpenQrDialog) },
                        ),
                        ActionSheetItem(
                            label = "Rename album",
                            onClick = { onUiEvent(AdminBeachAlbumDetailsUiEvent.OpenRenameAlbumDialog) },
                        ),
                        ActionSheetItem(
                            label = "Delete album",
                            onClick = { onUiEvent(AdminBeachAlbumDetailsUiEvent.OpenDeleteAlbumDialog) },
                            isDestructive = true,
                        ),
                    )
                }
                ActionSheet(
                    onDismiss = { onUiEvent(AdminBeachAlbumDetailsUiEvent.ToggleMoreActions) },
                    actionsList = actions,
                )
            }

            // Dialog handling
            when (val dialogState = content.dialogState) {
                is BeachAlbumDetailsDialogState.QrCode -> {
                    QrCodeDialog(
                        albumId = content.album.id,
                        beachName = content.album.name,
                        onDismiss = { onUiEvent(AdminBeachAlbumDetailsUiEvent.DismissDialog) },
                        onSaveImage = { cardBitmap ->
                            QrCodeGenerator.saveQrCardToStorage(context, content.album.name, cardBitmap)
                            onUiEvent(AdminBeachAlbumDetailsUiEvent.DismissDialog)
                        },
                        onShareImage = { cardBitmap ->
                            val saveResult = QrCodeGenerator.saveQrCardToStorage(context, content.album.name, cardBitmap)
                            if (saveResult is Result.Success) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, saveResult.data)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Beach QR Code"))
                            }
                            onUiEvent(AdminBeachAlbumDetailsUiEvent.DismissDialog)
                        },
                    )
                }
                is BeachAlbumDetailsDialogState.RenameAlbum -> {
                    ConfirmationDialogWithTextField(
                        onDismiss = { onUiEvent(AdminBeachAlbumDetailsUiEvent.DismissDialog) },
                        onConfirm = { onUiEvent(AdminBeachAlbumDetailsUiEvent.ConfirmRenameAlbum) },
                        dialogTitle = "Rename Album",
                        dialogMessage = "Enter a new name for the beach album:",
                        label = "Album Name",
                        textValue = dialogState.nameDraft,
                        onTextValueChange = { onUiEvent(AdminBeachAlbumDetailsUiEvent.RenameDraftChanged(it)) },
                        capitalization = true,
                        dismissButtonMessage = "Cancel",
                        confirmButtonMessage = "Rename",
                    )
                }
                is BeachAlbumDetailsDialogState.DeleteAlbum -> {
                    ConfirmationDialog(
                        onDismiss = { onUiEvent(AdminBeachAlbumDetailsUiEvent.DismissDialog) },
                        onConfirm = { onUiEvent(AdminBeachAlbumDetailsUiEvent.ConfirmDeleteAlbum) },
                        dialogTitle = "Delete Beach Album",
                        dialogMessage = "Are you sure you want to delete '${content.album.name}' and all its photos?",
                        dismissButtonMessage = "Cancel",
                        confirmButtonMessage = "Delete",
                    )
                }
                is BeachAlbumDetailsDialogState.DeleteSelectedMedia -> {
                    ConfirmationDialog(
                        onDismiss = { onUiEvent(AdminBeachAlbumDetailsUiEvent.DismissDialog) },
                        onConfirm = { onUiEvent(AdminBeachAlbumDetailsUiEvent.ConfirmDeleteSelectedMedia) },
                        dialogTitle = "Delete Photos",
                        dialogMessage = "Are you sure you want to delete ${content.selectedMediaIds.size} selected photo(s)?",
                        dismissButtonMessage = "Cancel",
                        confirmButtonMessage = "Delete",
                    )
                }
                null -> Unit
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminBeachAlbumDetailsScreenPreview() {
    LifeTogetherTheme {
        AdminBeachAlbumDetailsScreen(
            uiState = AdminBeachAlbumDetailsUiState.Content(
                album = BeachAlbum(id = "1", familyId = "1", name = "Bondi Beach", count = 3),
                mediaList = listOf(
                    BeachMedia(id = "1", familyId = "1", beachAlbumId = "1", url = "", storagePath = "", createdAt = Date(), lastUpdated = Date()),
                    BeachMedia(id = "2", familyId = "1", beachAlbumId = "1", url = "", storagePath = "", createdAt = Date(), lastUpdated = Date()),
                    BeachMedia(id = "3", familyId = "1", beachAlbumId = "1", url = "", storagePath = "", createdAt = Date(), lastUpdated = Date()),
                ),
            ),
            onUiEvent = {},
            onNavigationEvent = {},
        )
    }
}
