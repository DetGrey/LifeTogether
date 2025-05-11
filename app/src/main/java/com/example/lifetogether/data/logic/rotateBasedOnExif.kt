package com.example.lifetogether.data.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun ByteArray.rotateBasedOnExif(): ByteArray {
    try {
        val inputStream = ByteArrayInputStream(this)
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
            else -> 0 // No rotation needed
        }
        println("rotateBasedOnExif() Rotation: $rotation")

        val bitmap = BitmapFactory.decodeByteArray(this, 0, this.size)

        val rotatedBitmap = if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        // Convert back to ByteArray for Firebase upload
        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    } catch (e: Exception) {
        println("Error rotating image: ${e.message}")
        return this
    }
}

fun Uri.rotateBasedOnExif(context: Context): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        val byteArray = inputStream.readBytes()
        byteArray.rotateBasedOnExif()
    } catch (e: Exception) {
        println("Error rotating image: ${e.message}")
        null
    }
}
