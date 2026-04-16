package com.example.lifetogether.data.logic

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

private const val TAG = "GenerateThumbnail"

fun generateImageThumbnailFromFile(imageFile: File): ByteArray? {
    return try {
        // First, decode bounds to avoid loading the full image initially if it's huge
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile.absolutePath, options)

        // Calculate a reasonable inSampleSize to pre-scale the image if it's very large
        // This helps prevent OOMs before even applying your scaling logic.
        // Let's aim to not load anything excessively larger than screen width.
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        options.inSampleSize = calculateInSampleSize(options, screenWidth, screenWidth * 2) // Allow some flexibility
        options.inJustDecodeBounds = false

        val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)

        if (originalBitmap == null) {
            Log.e(TAG, "Failed to decode original bitmap from file: ${imageFile.path}")
            return null
        }

        val optimalWidth = screenWidth / 2 // Use this as the width limit for the thumbnail

        val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
        val scaledWidth: Int
        val scaledHeight: Int

        if (originalBitmap.width > originalBitmap.height) { // Landscape or square where width is determining factor
            scaledWidth = optimalWidth
            scaledHeight = (optimalWidth / aspectRatio).toInt().coerceAtLeast(1) // Ensure height is at least 1
        } else { // Portrait where height is determining factor based on optimalWidth for the other dimension
            scaledHeight = optimalWidth // If portrait, scale height to optimalWidth, width adjusts
            scaledWidth = (optimalWidth * aspectRatio).toInt().coerceAtLeast(1) // Ensure width is at least 1
        }

        // Ensure scaled dimensions are positive
        if (scaledWidth <= 0 || scaledHeight <= 0) {
            Log.w(TAG, "Calculated invalid scaled dimensions ($scaledWidth x $scaledHeight) for ${imageFile.name}. Skipping thumbnail generation.")
            // Don't try to load the entire file as a fallback - this can cause OOM
            return null
        }

        val thumbnailBitmap = originalBitmap.scale(scaledWidth, scaledHeight, true) // Use filter = true for better quality scaling
        originalBitmap.recycle() // Recycle the larger original bitmap

        ByteArrayOutputStream().use { outputStream ->
            thumbnailBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) // PNG for potentially better quality thumbnails
            thumbnailBitmap.recycle() // Recycle the scaled bitmap
            outputStream.toByteArray()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error generating image thumbnail from file ${imageFile.name}: ${e.message}", e)
        null // Return null on error instead of the original file bytes
    }
}

// Helper to calculate inSampleSize (you might have this already)
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun generateVideoThumbnailFromFile(videoFile: File): ByteArray? {
    var retriever: MediaMetadataRetriever? = null
    var originalFrame: Bitmap? = null
    var scaledThumbnailBitmap: Bitmap? = null
    return try {
        retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoFile.absolutePath)

        // Get a frame, e.g., at 1 second (1,000,000 microseconds)
        // OPTION_CLOSEST_SYNC is generally good for getting a usable keyframe.
        originalFrame = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

        if (originalFrame == null) {
            Log.w(TAG, "Could not retrieve frame from video: ${videoFile.name}")
            return null
        }

        // Now, scale this frame similar to how you scale images
        val displayMetrics = Resources.getSystem().displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val optimalWidth = screenWidth / 2

        val aspectRatio = originalFrame.width.toFloat() / originalFrame.height.toFloat()
        val scaledWidth: Int
        val scaledHeight: Int

        if (originalFrame.width > originalFrame.height) { // Landscape
            scaledWidth = optimalWidth
            scaledHeight = (optimalWidth / aspectRatio).toInt().coerceAtLeast(1)
        } else { // Portrait or square
            scaledHeight = optimalWidth
            scaledWidth = (optimalWidth * aspectRatio).toInt().coerceAtLeast(1)
        }

        if (scaledWidth <= 0 || scaledHeight <= 0) {
            Log.w(TAG, "Calculated invalid scaled dimensions for video frame ${videoFile.name}. Skipping thumbnail.")
            return null
        }

        scaledThumbnailBitmap = originalFrame.scale(scaledWidth, scaledHeight, true)

        ByteArrayOutputStream().use { outputStream ->
            // JPEG is generally better for video frame thumbnails in terms of size/quality balance
            scaledThumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // Quality 80
            outputStream.toByteArray()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error generating video thumbnail from file ${videoFile.name}: ${e.message}", e)
        null
    } finally {
        try {
            originalFrame?.recycle()
            scaledThumbnailBitmap?.recycle()
            retriever?.release() // Release the retriever
        } catch (ioe: IOException) {
            Log.e(TAG, "Exception while releasing MediaMetadataRetriever or recycling bitmaps: ${ioe.message}")
        }
    }
}
