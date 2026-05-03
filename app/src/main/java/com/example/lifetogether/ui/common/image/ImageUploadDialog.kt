package com.example.lifetogether.ui.common.image

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.toBitmap
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        ImageUploadSheetContent(
            title = dialogTitle,
            message = dialogMessage,
            bitmap = bitmap,
            error = error,
            isUploading = uploadState is UploadState.Uploading,
            dismissButtonMessage = dismissButtonMessage,
            confirmButtonMessage = confirmButtonMessage,
            onPickPhoto = { launcher.launch("image/*") },
            onDismiss = onDismiss,
            onConfirm = {
                val uri = selectedImageUri
                if (uri != null) {
                    uploadState = UploadState.Uploading
                    coroutineScope.launch {
                        when (val result = onUpload(uri)) {
                            is Result.Success -> {
                                selectedImageUri = null
                                bitmap = null
                                onConfirm()
                            }
                            is Result.Failure -> {
                                uploadState = UploadState.Idle
                                error = result.error.toUserMessage()
                            }
                        }
                    }
                } else {
                    error = "Please choose an image first"
                }
            },
        )
    }
}

@Composable
private fun ImageUploadSheetContent(
    title: String,
    message: String,
    bitmap: Bitmap?,
    error: String,
    isUploading: Boolean,
    dismissButtonMessage: String,
    confirmButtonMessage: String,
    onPickPhoto: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
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
            text = "Add Photo",
            onClick = onPickPhoto,
        )

        bitmap?.let { btm ->
            Image(
                bitmap = btm.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
            )
        }

        AnimatedVisibility(
            visible = error.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Text(error, color = MaterialTheme.colorScheme.error)
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
                enabled = bitmap != null,
                loading = isUploading,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageUploadSheetIdlePreview() {
    LifeTogetherTheme {
        Surface {
            ImageUploadSheetContent(
                title = "Upload photo",
                message = "Choose a photo to upload.",
                bitmap = null,
                error = "",
                isUploading = false,
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload",
                onPickPhoto = {},
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ImageUploadSheetErrorPreview() {
    LifeTogetherTheme {
        Surface {
            ImageUploadSheetContent(
                title = "Upload photo",
                message = "Choose a photo to upload.",
                bitmap = null,
                error = "Please choose an image first",
                isUploading = false,
                dismissButtonMessage = "Cancel",
                confirmButtonMessage = "Upload",
                onPickPhoto = {},
                onDismiss = {},
                onConfirm = {},
            )
        }
    }
}
