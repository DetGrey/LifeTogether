package com.example.lifetogether.data.logic

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants.BASE_BEACH_URL
import java.io.OutputStream

object QrCodeGenerator {

    fun getBeachAlbumUrl(albumId: String): String = "$BASE_BEACH_URL$albumId"

    /**
     * Composes a downloadable card image containing the QR code Bitmap with the beach name rendered below it.
     */
    fun createBeachQrCardBitmap(
        qrBitmap: Bitmap,
        beachName: String,
        width: Int = 1000,
        height: Int = 1200,
    ): Bitmap {
        val cardBitmap = createBitmap(width, height)
        val canvas = Canvas(cardBitmap)
        canvas.drawColor(Color.WHITE)

        // Draw padding
        val margin = 80f

        // Draw QR Code centered horizontally
        val qrTargetSize = (width - margin * 2).toInt()
        val scaledQr = qrBitmap.scale(qrTargetSize, qrTargetSize)
        val qrLeft = (width - qrTargetSize) / 2f
        val qrTop = margin
        canvas.drawBitmap(scaledQr, qrLeft, qrTop, null)

        // Draw Beach Name below QR Code
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 60f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val textY = qrTop + qrTargetSize + 120f
        canvas.drawText(beachName, width / 2f, textY, paint)

        // Draw subtitle/app name at very bottom
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.GRAY
            textSize = 32f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("LifeTogether Beach Album", width / 2f, textY + 60f, subPaint)

        return cardBitmap
    }

    /**
     * Saves the composite QR card bitmap to device storage via MediaStore.
     */
    fun saveQrCardToStorage(
        context: Context,
        beachName: String,
        cardBitmap: Bitmap,
    ): Result<Uri, AppError> {
        return try {
            val sanitizedName = beachName.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            val fileName = "Beach_QR_${sanitizedName}.png"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LifeTogether")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.Failure(AppError.Storage("Failed to create MediaStore entry"))

            val outputStream: OutputStream = resolver.openOutputStream(uri)
                ?: return Result.Failure(AppError.Storage("Failed to open output stream"))

            cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            showDownloadNotification(context, beachName, uri)
            Result.Success(uri)
        } catch (e: Exception) {
            Result.Failure(AppError.Storage(e.localizedMessage ?: "Failed to save QR card image"))
        }
    }

    private fun showDownloadNotification(
        context: Context,
        beachName: String,
        uri: Uri,
    ) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                ?: return

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/png")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, com.example.lifetogether.util.Constants.DEFAULT_CHANNEL)
                .setSmallIcon(com.example.lifetogether.R.drawable.ic_small_logo)
                .setContentTitle("Download complete")
                .setContentText("Saved beach QR code for '$beachName'")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (_: Exception) {
            // Notification error fallback
        }
    }
}
