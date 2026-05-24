package com.example.lifetogether.ui.common.image

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun EditableImageCard(
    bitmap: Bitmap?,
    isEditing: Boolean,
    onLaunchImagePicker: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.extraLarge,
            )
            .clickable(enabled = isEditing) {
                onLaunchImagePicker.invoke("image/*")
            },
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap == null) {
            TextDefault(
                text = if (isEditing) "Tap to add image" else "No image",
            )
        }
        AnimatedBitmapImage(
            bitmap = bitmap,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "entry image",
        )

        if (isEditing && bitmap != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(LifeTogetherTokens.spacing.small)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.small,
                    )
                    .padding(
                        horizontal = LifeTogetherTokens.spacing.small,
                        vertical = LifeTogetherTokens.spacing.xSmall,
                    ),
            ) {
                Text(
                    text = "Change image",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}