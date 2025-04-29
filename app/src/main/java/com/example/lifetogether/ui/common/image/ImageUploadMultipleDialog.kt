package com.example.lifetogether.ui.common.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.ui.viewmodel.ImageUploadViewModel

@Composable
fun ImageUploadMultipleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    dialogTitle: String,
    dialogMessage: String,
    imageType: ImageType,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
) {
    val viewModel: ImageUploadViewModel = hiltViewModel()
    val context = LocalContext.current
    val imageUris by viewModel.imageUris.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris: List<Uri>? ->
        uris?.let { viewModel.setImageUris(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = dialogMessage)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { launcher.launch("image/*") }) {
                    Text(text = "Add Photos")
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow {
                    items(imageUris) { uri ->
                        Image(
                            painter = rememberAsyncImagePainter(uri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(75.dp) // Square size
                                .clip(RectangleShape) // Crop to a square
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop // Center the image
                        )
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
                        onConfirm()
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
                onClick = { viewModel.uploadPhotos(imageType, context) },
            ) {
                Text(text = confirmButtonMessage)
            }
        },
    )
}
