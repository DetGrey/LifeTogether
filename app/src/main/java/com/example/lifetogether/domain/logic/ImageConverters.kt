package com.example.lifetogether.domain.logic

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException

fun ByteArray.toBitmap(): Bitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size)
}

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun Uri.toBitmap(contentResolver: ContentResolver): Bitmap? {
    return try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, this))
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}
