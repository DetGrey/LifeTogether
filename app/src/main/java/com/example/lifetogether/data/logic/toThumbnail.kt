package com.example.lifetogether.data.logic

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream

fun ByteArray.toThumbnail(): ByteArray {
    try {
        val originalBitmap = BitmapFactory.decodeByteArray(this, 0, this.size)
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val optimalSize = screenWidth / 2 // Use this as the width limit

        // Calculate the scaled height while maintaining aspect ratio
        val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val scaledWidth: Int
        val scaledHeight: Int

        if (aspectRatio > 1) { // Landscape image
            scaledWidth = optimalSize
            scaledHeight = (optimalSize / aspectRatio).toInt()
        } else { // Portrait or square image
            scaledWidth = (optimalSize * aspectRatio).toInt()
            scaledHeight = optimalSize
        }

        val thumbnailBitmap = originalBitmap.scale(scaledWidth, scaledHeight, false)

        val outputStream = ByteArrayOutputStream()
        thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    } catch (e: Exception) {
        println("Error converting image to thumbnail: ${e.message}")
        return this
    }
}
