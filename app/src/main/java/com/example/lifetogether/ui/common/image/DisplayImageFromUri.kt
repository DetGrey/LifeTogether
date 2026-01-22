package com.example.lifetogether.ui.common.image

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage

@Composable
fun DisplayImageFromUri(imageUri: Uri, description: String?) {
    ZoomableAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .crossfade(true)
            .build(),
        contentDescription = description ?: "gallery image",
        modifier = Modifier.fillMaxSize(), // Important for zoom boundaries
        contentScale = ContentScale.Fit, // Keeps horizontal images correctly sized
    )
}
