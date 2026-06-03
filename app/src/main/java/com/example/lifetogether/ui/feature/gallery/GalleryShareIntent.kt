package com.example.lifetogether.ui.feature.gallery

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.lifetogether.domain.model.gallery.ShareableGalleryMedia

fun shareMediaIntent(
    context: Context,
    media: List<ShareableGalleryMedia>,
): Intent {
    return if (media.size == 1) {
        val item = media.first()
        Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, item.uri)
            putExtra(Intent.EXTRA_TITLE, item.fileName)
            clipData = ClipData.newUri(context.contentResolver, item.fileName, item.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = media.shareMimeType()
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList<Uri>(media.map { it.uri }))
            clipData = media.toClipData(context)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

private fun List<ShareableGalleryMedia>.shareMimeType(): String {
    return when {
        all { it.mimeType.startsWith("image/") } -> "image/*"
        all { it.mimeType.startsWith("video/") } -> "video/*"
        else -> "*/*"
    }
}

private fun List<ShareableGalleryMedia>.toClipData(context: Context): ClipData {
    val first = first()
    return ClipData.newUri(context.contentResolver, first.fileName, first.uri).apply {
        drop(1).forEach { item ->
            addItem(ClipData.Item(item.uri))
        }
    }
}
