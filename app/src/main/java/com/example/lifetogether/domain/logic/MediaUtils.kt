package com.example.lifetogether.domain.logic

// In your ...ui.common.image package or a utility file

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.IOException

fun isImageUri(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    return mimeType?.startsWith("image/") == true
}

fun isVideoUri(context: Context, uri: Uri): Boolean {
    val mimeType = context.contentResolver.getType(uri)
    return mimeType?.startsWith("video/") == true
}

fun getVideoThumbnail(context: Context, uri: Uri, timeUs: Long = 1_000_000L): Bitmap? {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        // Retrieve a frame at a specific time (e.g., 1 second into the video)
        // OPTION_CLOSEST_SYNC is generally recommended for performance and accuracy
        return retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    } catch (ex: IllegalArgumentException) {
        Log.e("GetVideoThumbnail", "IllegalArgumentException for URI: $uri", ex)
    } catch (ex: RuntimeException) {
        Log.e("GetVideoThumbnail", "RuntimeException for URI: $uri (often related to invalid data source)", ex)
    } catch (ex: IOException) {
        Log.e("GetVideoThumbnail", "IOException for URI: $uri (cannot release retriever)", ex)
    } finally {
        try {
            retriever.release()
        } catch (ex: IOException) {
            Log.e("GetVideoThumbnail", "Error releasing MediaMetadataRetriever", ex)
        }
    }
    return null
}
