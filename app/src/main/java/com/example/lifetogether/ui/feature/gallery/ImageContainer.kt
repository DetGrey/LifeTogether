package com.example.lifetogether.ui.feature.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R

@Composable
fun RowScope.ImageContainer(
    bitmap: Bitmap? = null,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape = RoundedCornerShape(10))
            .background(color = MaterialTheme.colorScheme.onBackground)
            .weight(0.5f),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_gallery),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
            )
        }
    }
}