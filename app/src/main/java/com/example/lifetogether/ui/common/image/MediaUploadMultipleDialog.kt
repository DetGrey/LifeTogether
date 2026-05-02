package com.example.lifetogether.ui.common.image

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.lifetogether.domain.logic.getVideoThumbnail
import com.example.lifetogether.domain.logic.isImageUri
import com.example.lifetogether.domain.logic.isVideoUri
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaUploadMultipleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpload: suspend (List<Uri>) -> Result<Unit, AppError>,
    dialogTitle: String,
    dialogMessage: String,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedMediaUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var videoThumbnails by remember { mutableStateOf<Map<Uri, Bitmap?>>(emptyMap()) }
    var uploadState by remember { mutableStateOf<UploadState>(UploadState.Idle) }
    var error by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri>? ->
        uris?.let { selectedUris ->
            val validMediaUris = selectedUris.filter { uri ->
                val mimeType = context.contentResolver.getType(uri)
                mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true
            }
            if (validMediaUris.isNotEmpty()) {
                selectedMediaUris = validMediaUris
                uploadState = UploadState.Idle
                error = ""

                val currentUrisSet = validMediaUris.toSet()
                videoThumbnails = videoThumbnails.filterKeys { it in currentUrisSet }

                validMediaUris.forEach { uri ->
                    if (isVideoUri(context, uri) && !videoThumbnails.containsKey(uri)) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val thumbnail = getVideoThumbnail(context, uri)
                            withContext(Dispatchers.Main) {
                                videoThumbnails = videoThumbnails + (uri to thumbnail)
                            }
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        MediaUploadSheetContent(
            title = dialogTitle,
            message = dialogMessage,
            selectedMediaUris = selectedMediaUris,
            videoThumbnails = videoThumbnails,
            error = error,
            isUploading = uploadState is UploadState.Uploading,
            dismissButtonMessage = dismissButtonMessage,
            confirmButtonMessage = confirmButtonMessage,
            onPickMedia = { launcher.launch("*/*") },
            onDismiss = onDismiss,
            onConfirm = {
                if (selectedMediaUris.isEmpty()) {
                    error = "No files selected."
                    return@MediaUploadSheetContent
                }

                uploadState = UploadState.Uploading
                error = ""

                coroutineScope.launch(Dispatchers.IO) {
                    when (val result = onUpload(selectedMediaUris)) {
                        is Result.Success -> {
                            withContext(Dispatchers.Main) {
                                selectedMediaUris = emptyList()
                                videoThumbnails = emptyMap()
                                onConfirm()
                            }
                        }
                        is Result.Failure -> {
                            withContext(Dispatchers.Main) {
                                uploadState = UploadState.Idle
                                error = result.error.toUserMessage()
                            }
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun MediaUploadSheetContent(
    title: String,
    message: String,
    selectedMediaUris: List<Uri>,
    videoThumbnails: Map<Uri, Bitmap?>,
    error: String,
    isUploading: Boolean,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
    onPickMedia: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
    ) {
        TextSubHeadingMedium(text = title)

        TextDefault(text = message)

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = "Add Photos & Videos",
            onClick = onPickMedia,
        )

        if (selectedMediaUris.isNotEmpty()) {
            LazyRow {
                items(selectedMediaUris) { uri ->
                    Box(
                        modifier = Modifier
                            .size(75.dp)
                            .clip(RectangleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isImageUri(context, uri)) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else if (isVideoUri(context, uri)) {
                            val thumbnailBitmap = videoThumbnails[uri]
                            if (thumbnailBitmap != null) {
                                Image(
                                    bitmap = thumbnailBitmap.asImageBitmap(),
                                    contentDescription = "Video Thumbnail",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Video Icon",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            Text("?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(modifier = Modifier.width(LifeTogetherTokens.spacing.small))
                }
            }
        }

        AnimatedVisibility(
            visible = error.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            SecondaryButton(
                modifier = Modifier.weight(1f),
                text = dismissButtonMessage,
                onClick = onDismiss,
            )
            PrimaryButton(
                modifier = Modifier.weight(1f),
                text = confirmButtonMessage,
                onClick = onConfirm,
                loading = isUploading,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaUploadSheetIdlePreview() {
    LifeTogetherTheme {
        Surface {
            MediaUploadSheetContent(
                title = "Upload media",
                message = "Choose photos or videos to upload.",
                selectedMediaUris = emptyList(),
                videoThumbnails = emptyMap(),
                error = "",
                isUploading = false,
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload",
                onPickMedia = {},
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MediaUploadSheetErrorPreview() {
    LifeTogetherTheme {
        Surface {
            MediaUploadSheetContent(
                title = "Upload media",
                message = "Choose photos or videos to upload.",
                selectedMediaUris = emptyList(),
                videoThumbnails = emptyMap(),
                error = "No files selected.",
                isUploading = false,
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload",
                onPickMedia = {},
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}
