package com.example.lifetogether.domain.converter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("LifeTogether Copied Text", text)
    clipboard.setPrimaryClip(clip)
    println("Text copied to clipboard: $text")
}
