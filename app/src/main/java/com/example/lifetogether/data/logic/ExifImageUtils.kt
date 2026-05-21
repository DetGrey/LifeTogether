package com.example.lifetogether.data.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File

object ExifImageUtils {
    private const val TAG = "ExifImageUtils"
    private const val JPEG_QUALITY = 85

    fun readOriginalBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read image bytes: ${e.message}", e)
            null
        }
    }

    fun readExifOrientation(imageBytes: ByteArray): Int? {
        return try {
            ByteArrayInputStream(imageBytes).use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read EXIF orientation: ${e.message}")
            null
        }
    }

    fun rotateAndEncodeJpeg(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)

                val rotation = rotationDegrees(
                    exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                )

                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                options.inSampleSize = if (options.outWidth > 2048 || options.outHeight > 2048) {
                    4
                } else if (options.outWidth > 1024 || options.outHeight > 1024) {
                    2
                } else {
                    1
                }
                options.inJustDecodeBounds = false

                val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                } ?: return null

                val rotatedBitmap = if (rotation != 0f) {
                    val matrix = Matrix().apply { postRotate(rotation) }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                        bitmap.recycle()
                    }
                } else {
                    bitmap
                }

                ByteArrayOutputStream().use { outputStream ->
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
                    rotatedBitmap.recycle()
                    outputStream.toByteArray()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating image: ${e.message}", e)
            null
        }
    }

    fun rotateBitmapIfNeeded(bitmap: Bitmap, imageFile: File): Bitmap {
        return try {
            val exif = ExifInterface(imageFile.absolutePath)
            val rotation = rotationDegrees(
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            )

            if (rotation == 0f) {
                bitmap
            } else {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    .also { bitmap.recycle() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply EXIF rotation for ${imageFile.name}: ${e.message}")
            bitmap
        }
    }

    private fun rotationDegrees(orientation: Int): Float {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
}
