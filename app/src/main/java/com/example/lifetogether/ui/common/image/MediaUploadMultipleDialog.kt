package com.example.lifetogether.ui.common.image

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.domain.logic.getVideoThumbnail
import com.example.lifetogether.domain.logic.isImageUri
import com.example.lifetogether.domain.logic.isVideoUri
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = dialogMessage)
                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))
                Button(onClick = { launcher.launch("*/*") }) {
                    Text(text = "Add Photos & Videos")
                }
                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))
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
                        Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.small))
                    }
                }

                when (uploadState) {
                    is UploadState.Uploading -> CircularProgressIndicator()
                    is UploadState.Success -> {
                        Text(text = "Upload Successful")
                    }
                    is UploadState.Failure -> {
                        Text(text = "Upload Failed: ${(uploadState as UploadState.Failure).error}")
                    }
                    else -> {}
                }

                if (error.isNotBlank()) {
                    Text(text = error)
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = dismissButtonMessage)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (uploadState is UploadState.Uploading) {
                        return@Button
                    }

                    if (selectedMediaUris.isEmpty()) {
                        uploadState = UploadState.Failure("No files selected.")
                        return@Button
                    }

                    uploadState = UploadState.Uploading
                    error = ""

                    coroutineScope.launch(Dispatchers.IO) {
                        when (val result = onUpload(selectedMediaUris)) {
                            is Result.Success -> {
                                withContext(Dispatchers.Main) {
                                    uploadState = UploadState.Success
                                    selectedMediaUris = emptyList()
                                    videoThumbnails = emptyMap()
                                    onConfirm()
                                }
                            }

                            is Result.Failure -> {
                                val message = result.error.toUserMessage()
                                withContext(Dispatchers.Main) {
                                    error = message
                                    uploadState = UploadState.Failure(message)
                                }
                            }
                        }
                    }
                },
            ) {
                Text(text = confirmButtonMessage)
            }
        },
    )
}
