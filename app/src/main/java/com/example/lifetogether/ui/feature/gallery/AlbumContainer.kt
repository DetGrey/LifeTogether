package com.example.lifetogether.ui.feature.gallery

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.example.lifetogether.ui.common.text.TextBodyLarge
import com.example.lifetogether.ui.common.text.TextDefault

@Composable
fun AlbumContainer(
    albumName: String,
    count: Int = 0,
    bitmap: Bitmap? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.5f)
            .padding(10.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f) // Maintain aspect ratio
                .clip(shape = RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.onBackground),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "image",
                    contentScale = ContentScale.Crop,
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_gallery),
                    contentDescription = "gallery icon",
                    contentScale = ContentScale.Crop,

                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextBodyLarge(
            text = albumName,
            modifier = Modifier
                .padding(start = 5.dp)
        )

        TextDefault(
            text = count.toString(),
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}