package com.example.lifetogether.ui.feature.admin.beachAlbums.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lifetogether.data.logic.QrCodeGenerator
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
fun QrCodeDialog(
    albumId: String,
    beachName: String,
    onDismiss: () -> Unit,
    onSaveImage: (cardBitmap: Bitmap) -> Unit,
    onShareImage: (cardBitmap: Bitmap) -> Unit,
) {
    val qrUrl = QrCodeGenerator.getBeachAlbumUrl(albumId)
    val painter = rememberQrCodePainter(qrUrl)
    val density = LocalDensity.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(LifeTogetherTokens.spacing.small),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(LifeTogetherTokens.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Beach Album QR Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.medium))

                // Card container rendering QR code + Beach name
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ComposeColor.White,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .padding(LifeTogetherTokens.spacing.medium),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painter,
                        contentDescription = "QR Code for $beachName",
                        modifier = Modifier.size(240.dp),
                    )

                    Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))

                    Text(
                        text = beachName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.Black,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = "Scan to view photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = ComposeColor.Gray,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.large))

                // Render card bitmap helper for saving/sharing
                val renderBitmap: () -> Bitmap = {
                    val imageBitmap = ImageBitmap(800, 800)
                    val composeCanvas = ComposeCanvas(imageBitmap)
                    val drawScope = CanvasDrawScope()
                    drawScope.draw(
                        density = density,
                        layoutDirection = LayoutDirection.Ltr,
                        canvas = composeCanvas,
                        size = Size(800f, 800f),
                    ) {
                        with(painter) {
                            draw(Size(800f, 800f))
                        }
                    }
                    val baseQr = imageBitmap.asAndroidBitmap()
                    QrCodeGenerator.createBeachQrCardBitmap(
                        qrBitmap = baseQr,
                        beachName = beachName,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    OutlinedButton(
                        onClick = { onSaveImage(renderBitmap()) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Save")
                    }

                    Button(
                        onClick = { onShareImage(renderBitmap()) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Share")
                    }
                }

                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Close")
                }
            }
        }
    }
}
