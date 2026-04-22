package com.example.lifetogether.domain.logic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log

private const val TAG = "ClipboardUtils"

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("LifeTogether Copied Text", text)
    clipboard.setPrimaryClip(clip)
    Log.d(TAG, "Text copied to clipboard")
}
