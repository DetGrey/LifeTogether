package com.example.lifetogether.ui.common.image

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun DisplayImageFromUri(imageUri: Uri, description: String?) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .crossfade(true) // Optional: for a nice fade-in animation
            // .size(width = 500, height = 500) // Optional: Explicitly set size if needed for downsampling before display
            // .placeholder(R.drawable.placeholder_image) // Optional: Your placeholder drawable
            // .error(R.drawable.error_image) // Optional: Your error drawable
            .build(),
        contentDescription = description ?: "gallery image",
        modifier = Modifier
            .fillMaxWidth(),
//            .aspectRatio(1f) // Adjust aspect ratio as needed
//            .padding(16.dp),
        contentScale = ContentScale.Crop, // Or ContentScale.Fit, etc.
    )
}
    