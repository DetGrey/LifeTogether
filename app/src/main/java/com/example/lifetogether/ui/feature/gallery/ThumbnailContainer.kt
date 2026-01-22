package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.durationToString
import com.example.lifetogether.ui.common.text.TextDefault

@Composable
fun ThumbnailContainer(
    thumbnail: ByteArray?,
    onClick: () -> Unit,
    isVideo: Boolean = false,
    duration: Long? = null,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10))
            .background(MaterialTheme.colorScheme.onBackground)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnail != null) {
            // Use Coil for memory-efficient loading with automatic bitmap pooling
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_gallery),
                contentDescription = null,
            )
        }
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.45f),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "play arrow",
                )
            }
        }
        if (duration != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 5.dp, end = 7.dp),
                contentAlignment = Alignment.BottomEnd,
            ) {
                TextDefault(durationToString(duration), color = Color.White)
            }
        }
    }
}
