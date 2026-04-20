package com.example.lifetogether.data.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

fun Uri.rotateBasedOnExif(context: Context): ByteArray? {
    return try {
        context.contentResolver.openInputStream(this)?.use { inputStream ->
            // Stream the input instead of loading entire file into memory first
            val exif = ExifInterface(inputStream)
            
            val rotation = when (
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            
            // Now read the image with inSampleSize if it's large
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            
            context.contentResolver.openInputStream(this)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            
            // Calculate sample size to avoid OOM
            options.inSampleSize = if (options.outWidth > 2048 || options.outHeight > 2048) {
                4 // Reduce by 4x if very large
            } else if (options.outWidth > 1024 || options.outHeight > 1024) {
                2 // Reduce by 2x if moderately large
            } else {
                1 // Full size if reasonably small
            }
            options.inJustDecodeBounds = false
            
            val bitmap = context.contentResolver.openInputStream(this)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return null
            
            val rotatedBitmap = if (rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    bitmap.recycle()
                }
            } else {
                bitmap
            }
            
            ByteArrayOutputStream().use { outputStream ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                rotatedBitmap.recycle()
                outputStream.toByteArray()
            }
        }
    } catch (e: Exception) {
        Log.e("rotateBasedOnExif", "Error rotating image: ${e.message}", e)
        null
    }
}
