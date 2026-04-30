package com.example.lifetogether.ui.feature.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.durationToString
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun ThumbnailContainer(
    thumbnail: ByteArray?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isVideo: Boolean = false,
    duration: Long? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectionToggle: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
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
            Icon(
                painter = painterResource(id = R.drawable.ic_gallery),
                contentDescription = null,
            )
        }
        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize(0.45f),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "play arrow",
                )
            }
        }
        if (duration != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = LifeTogetherTokens.spacing.xSmall, end = LifeTogetherTokens.spacing.small),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                TextDefault(duration.durationToString(), color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }
        if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(LifeTogetherTokens.sizing.touchTargetMinimum)
                        .align(Alignment.TopStart)
                        .padding(LifeTogetherTokens.spacing.xSmall),
                ) {
                CompletableBox(
                    isCompleted = isSelected,
                    onCompleteToggle = onSelectionToggle,
                )
            }
        }
    }
}
