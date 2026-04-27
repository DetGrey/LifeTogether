package com.example.lifetogether.ui.common.image

import android.graphics.Bitmap
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import kotlinx.coroutines.launch

@Composable
fun ImageUploadDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpload: suspend (Uri) -> Result<Unit, AppError>,
    dialogTitle: String,
    dialogMessage: String,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var uploadState by remember { mutableStateOf<UploadState>(UploadState.Idle) }
    var error by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            bitmap = it.toBitmap(context.contentResolver)
            error = ""
            uploadState = UploadState.Idle
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = dialogTitle) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = dialogMessage)

                Spacer(modifier = Modifier.height(10.dp))

                Button(onClick = { launcher.launch("image/*") }) {
                    Text(text = "Add Photo")
                }

                bitmap?.let { btm ->
                    error = ""
                    Image(
                        bitmap = btm.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(128.dp),
                    )
                }

                if (error.isNotEmpty()) {
                    Text(error)
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
                    val uri = selectedImageUri
                    if (uri != null) {
                        uploadState = UploadState.Uploading
                        coroutineScope.launch {
                            when (val result = onUpload(uri)) {
                                is Result.Success -> {
                                    uploadState = UploadState.Success
                                    selectedImageUri = null
                                    bitmap = null
                                    onConfirm()
                                }

                                is Result.Failure -> {
                                    val message = result.error.toUserMessage()
                                    error = message
                                    uploadState = UploadState.Failure(message)
                                }
                            }
                        }
                    } else {
                        error = "Please choose an image first"
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
