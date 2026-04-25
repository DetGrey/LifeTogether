package com.example.lifetogether.ui.common.image

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.sealed.ImageState
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.di.rememberObserveImageStateUseCase

@Composable
fun rememberObservedImageBitmap(
    imageType: ImageType?,
    onError: (String) -> Unit = {},
): Bitmap? {
    val observeImageStateUseCase = rememberObserveImageStateUseCase()
    val currentOnError by rememberUpdatedState(onError)
    val imageState by observeImageStateUseCase(imageType).collectAsStateWithLifecycle(
        initialValue = if (imageType == null) ImageState.Empty else ImageState.Loading,
    )

    LaunchedEffect(imageState) {
        val error = imageState as? ImageState.Error ?: return@LaunchedEffect
        currentOnError(error.message)
    }

    return (imageState as? ImageState.Loaded)?.bytes?.toBitmap()
}