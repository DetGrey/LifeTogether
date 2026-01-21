package com.example.lifetogether.ui.common.image

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.lifetogether.domain.logic.isImageUri
import com.example.lifetogether.domain.logic.isVideoUri
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.sealed.UploadState

@Composable
fun MediaUploadMultipleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogMessage: String,
    imageType: ImageType.GalleryMedia,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
) {
    val viewModel: MediaUploadViewModel = hiltViewModel()
    val context = LocalContext.current
    val selectedMediaUris by viewModel.selectedMediaUris.collectAsState()
    val videoThumbnails by viewModel.videoThumbnails.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri>? ->
        uris?.let { selectedUris ->
            // Filter the URIs here
            val validMediaUris = selectedUris.filter { uri ->
                val mimeType = context.contentResolver.getType(uri)
                mimeType?.startsWith("image/") == true || mimeType?.startsWith("video/") == true
            }
            if (validMediaUris.isNotEmpty()) {
                viewModel.setSelectedMediaUris(validMediaUris, context)
            } else {
                // Handle case where no valid images or videos were selected
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = dialogMessage)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { launcher.launch("*/*") }) {
                    Text(text = "Add Photos & Videos")
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow {
                    items(selectedMediaUris) { uri ->
                        Box(
                            modifier = Modifier
                                .size(75.dp)
                                .clip(RectangleShape)
                                .background(Color.Gray),
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
                                    // Show a placeholder or loading for video thumbnail
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Video Icon",
                                        tint = Color.White,
                                    )
                                    // Could also show a mini CircularProgressIndicator here while thumbnail generates
                                }
                            } else {
                                // Fallback for unknown types or if type detection fails
                                Text("?", color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                when (uploadState) {
                    is UploadState.Uploading -> CircularProgressIndicator()
                    is UploadState.Success -> {
                        Text(text = "Upload Successful")
                        viewModel.resetViewModel()
                        onConfirm()
                    }
                    is UploadState.Failure -> {
                        Text(text = "Upload Failed: ${(uploadState as UploadState.Failure).error}")
                        onDismiss()
                    }
                    else -> {}
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
                onClick = { viewModel.uploadMediaItems(imageType.familyId, imageType.albumId, context) },
            ) {
                Text(text = confirmButtonMessage)
            }
        },
    )
}
