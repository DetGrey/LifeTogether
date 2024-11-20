package com.example.lifetogether.ui.common.image

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.ui.viewmodel.ImageUploadViewModel

@Composable
fun ImageUploadDialog(
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
    val bitmap by viewModel.bitmap.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.setImageUri(it, context.contentResolver) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column {
                Text(text = dialogMessage)

                Spacer(modifier = Modifier.height(10.dp))

                Button(onClick = { launcher.launch("image/*") }) {
                    Text(text = "Add Photo")
                }

                bitmap?.let { btm ->
                    viewModel.error = ""
                    Image(
                        bitmap = btm.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                    )
                }

                if (viewModel.error.isNotEmpty()) {
                    Text(viewModel.error)
                }

                when (uploadState) {
                    is UploadState.Uploading -> CircularProgressIndicator()
                    is UploadState.Success -> {
                        Text(text = "Upload Successful")
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
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = dismissButtonMessage,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bitmap != null) {
                        viewModel.uploadPhoto(imageType, context)
                    } else {
                        viewModel.error = "Please choose an image first"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = confirmButtonMessage,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    )
}
