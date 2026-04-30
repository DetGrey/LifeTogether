package com.example.lifetogether.ui.feature.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.text.TextBodyLarge
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun AlbumCard(
    albumName: String,
    count: Int = 0,
    bitmap: Bitmap? = null,
    onClick: () -> Unit = {},
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .padding(LifeTogetherTokens.spacing.small)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground),
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gallery),
                    contentDescription = "gallery icon",
                )
            }
        }
        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))
        TextBodyLarge(
            text = albumName,
            modifier = Modifier
                .padding(start = LifeTogetherTokens.spacing.xSmall),
        )

        TextDefault(
            text = count.toString(),
            modifier = Modifier.padding(start = LifeTogetherTokens.spacing.xSmall),
        )
    }
}
