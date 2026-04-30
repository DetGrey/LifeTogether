package com.example.lifetogether.ui.feature.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.text.TextBodyLarge
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun AlbumCard(
    albumName: String,
    count: Int = 0,
    bitmap: Bitmap? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .padding(LifeTogetherTokens.spacing.small)
            .clickable { onClick() },
    ) {
        Card(
            modifier = Modifier
                .aspectRatio(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = MaterialTheme.shapes.large,
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

@Preview(showBackground = true)
@Composable
private fun AlbumCardPreview() {
    LifeTogetherTheme {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = 2,
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            AlbumCard(
                albumName = "Weekend trip",
                count = 12,
            )
        }
    }
}
