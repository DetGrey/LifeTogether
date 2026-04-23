package com.example.lifetogether.ui.common.image // Or your desired package

import com.example.lifetogether.domain.result.toUserMessage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.logic.getVideoThumbnail
import com.example.lifetogether.domain.logic.isVideoUri
import com.example.lifetogether.domain.logic.parseExifDate
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.gallery.MediaUploadData
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.domain.usecase.image.UploadGalleryMediaItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class MediaUploadViewModel @Inject constructor(
    private val uploadGalleryMediaItemsUseCase: UploadGalleryMediaItemsUseCase,
) : ViewModel() {
    var error: String by mutableStateOf("")

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun resetViewModel() {
        _selectedMediaUris.value = emptyList()
        _videoThumbnails.value = emptyMap()
        _uploadState.value = UploadState.Idle
        error = ""
    }

    // ---------------------------------------------------------------- MULTIPLE MEDIA
    private val _selectedMediaUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedMediaUris: StateFlow<List<Uri>> = _selectedMediaUris.asStateFlow()

    private val _videoThumbnails = MutableStateFlow<Map<Uri, Bitmap?>>(emptyMap())
    val videoThumbnails: StateFlow<Map<Uri, Bitmap?>> = _videoThumbnails.asStateFlow()

    fun setSelectedMediaUris(uris: List<Uri>, context: Context) {
        _selectedMediaUris.value = uris
        _uploadState.value = UploadState.Idle // Reset state when new files are selected
        error = ""

        // Clear previous thumbnails for URIs no longer selected (optional, but good practice)
        val currentUrisSet = uris.toSet()
        _videoThumbnails.update { currentThumbnails ->
            currentThumbnails.filterKeys { it in currentUrisSet }
        }

        // Launch thumbnail generation for new video URIs
        viewModelScope.launch {
            uris.forEach { uri ->
                if (isVideoUri(context, uri) && _videoThumbnails.value[uri] == null) {
                    generateThumbnailForUri(uri, context)
                }
            }
        }
    }

    private fun generateThumbnailForUri(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) { // Perform heavy work on IO dispatcher
            Log.d("ViewModel", "Generating thumbnail for: $uri")
            val thumbnail = getVideoThumbnail(context, uri)
            withContext(Dispatchers.Main) { // Update StateFlow on Main thread
                if (thumbnail != null) {
                    _videoThumbnails.update { currentMap ->
                        currentMap + (uri to thumbnail)
                    }
                    Log.d("ViewModel", "Thumbnail generated successfully for: $uri")
                } else {
                    _videoThumbnails.update { currentMap ->
                        currentMap + (uri to null) // Explicitly store null if generation failed
                    }
                    Log.w("ViewModel", "Failed to generate thumbnail for: $uri")
                }
            }
        }
    }

    // Main function to upload selected images and videos
    fun uploadMediaItems(
        familyId: String,
        albumId: String,
        context: Context,
    ) {
        if (uploadState.value is UploadState.Uploading) {
            Log.d("MediaUploadVM", "Upload already in progress.")
            return
        }

        val urisToUpload = selectedMediaUris.value
        if (urisToUpload.isEmpty()) {
            Log.d("MediaUploadVM", "No media URIs to upload.")
            _uploadState.value = UploadState.Failure("No files selected.")
            return
        }

        _uploadState.value = UploadState.Uploading

        viewModelScope.launch(Dispatchers.IO) {
            val mediaUploadDataList = mutableListOf<MediaUploadData>()

            for (uri in urisToUpload) {
                val mimeType = context.contentResolver.getType(uri)

                when {
                    mimeType?.startsWith("image/") == true -> {
                        val extension = getMimeTypeExtension(mimeType) ?: ".jpeg"
                        val metadata = extractImageMetadata(context, uri, familyId, albumId, extension)
                        mediaUploadDataList.add(
                            MediaUploadData.ImageUpload(
                                uri = uri,
                                mediaType = metadata.first,
                                extension = metadata.second,
                            ),
                        )
                    }
                    mimeType?.startsWith("video/") == true -> {
                        val extension = getMimeTypeExtension(mimeType) ?: ".mp4"
                        val metadata = extractVideoMetadata(context, uri, familyId, albumId, extension)
                        mediaUploadDataList.add(
                            MediaUploadData.VideoUpload(
                                uri = uri,
                                mediaType = metadata.first,
                                extension = metadata.second,
                            ),
                        )
                    }
                    else -> {
                        Log.w("MediaUploadVM", "Unsupported MimeType $mimeType for URI: $uri")
                        // Optionally add to a list of unsupported files or show an error
                    }
                }
            }

            if (mediaUploadDataList.isEmpty() && urisToUpload.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    _uploadState.value = UploadState.Failure("No supported files found to upload.")
                }
                return@launch
            }
            if (mediaUploadDataList.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _uploadState.value = UploadState.Idle
                }
                return@launch
            }

            Log.d("MediaUploadVM", "Prepared MediaUploadData: $mediaUploadDataList")

            // The UploadMediaItemsUseCase will take this list and handle uploading each item
            // It will also be responsible for creating Firestore/Room entries
            when (val result = uploadGalleryMediaItemsUseCase.invoke(mediaUploadDataList, context)) {
                is Result.Success -> {
                    withContext(Dispatchers.Main) {
                        _uploadState.value = UploadState.Success
                    }
                    Log.d("MediaUploadVM", "Upload successful for all items.")
                }
                is Result.Failure -> {
                    withContext(Dispatchers.Main) {
                        _uploadState.value = UploadState.Failure(result.error.toUserMessage())
                        error = result.error.toUserMessage()
                    }
                    Log.e("MediaUploadVM", "Upload failed: ${result.error.toUserMessage()}")
                }
            }
        }
    }

    // --- Metadata Extraction ---

    private suspend fun extractImageMetadata(
        context: Context,
        uri: Uri,
        familyId: String,
        albumId: String,
        ext: String,
    ): Pair<GalleryImage, String> = withContext(Dispatchers.IO) {
        val dateCreated = getBestDate(context, uri)
        val itemName = formatMediaName(dateCreated, ext)

        val galleryImage = GalleryImage(
            familyId = familyId,
            itemName = itemName,
            albumId = albumId,
            dateCreated = dateCreated,
        )
        Pair(galleryImage, ext)
    }

    private suspend fun extractVideoMetadata(
        context: Context,
        uri: Uri,
        familyId: String,
        albumId: String,
        ext: String,
    ): Pair<GalleryVideo, String> {
        var duration: Long? = null
        var dateCreated: Date? = null

        withContext(Dispatchers.IO) { // Ensure MediaMetadataRetriever runs on IO thread
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationStr?.toLongOrNull()

                val dateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
                // Date format from METADATA_KEY_DATE is typically "yyyyMMdd'T'HHmmss.SSS'Z'"
                // You might need a specific parser if it's different or use file modification date as fallback
                if (dateStr != null) {
                    try {
                        // Attempt to parse if in a common format, e.g. "20230415T102030.000Z"
                        // This parsing is basic, adjust as needed.
                        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.UK)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        dateCreated = sdf.parse(dateStr)
                    } catch (e: Exception) {
                        Log.w("MediaUploadVM", "Could not parse video date metadata: $dateStr", e)
                    }
                }
                // Fallback to file's last modified date if EXIF-like date is not available or parsable
                dateCreated = dateCreated ?: getFileLastModifiedDate(context, uri)
            } catch (e: Exception) {
                Log.e("MediaUploadVM", "Error extracting video metadata for $uri", e)
            } finally {
                try {
                    retriever.release()
                } catch (e: IOException) {
                    Log.e("MediaUploadVM", "Error releasing retriever for video metadata", e)
                }
            }
        }
        dateCreated = dateCreated ?: Date() // Absolute fallback if no other date found

        val itemName = formatMediaName(dateCreated, ext)

        val galleryVideo = GalleryVideo(
            // id will be generated by Firestore or your use case
            familyId = familyId,
            itemName = itemName,
            albumId = albumId,
            dateCreated = dateCreated,
            duration = duration,
            // mediaUrl & thumbnailUrl (if separate) will be set by use case after upload
        )
        return Pair(galleryVideo, ext)
    }

    private fun getFileLastModifiedDate(context: Context, uri: Uri): Date? {
        try {
            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val dateModifiedSeconds = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED))
                        return Date(dateModifiedSeconds * 1000) // Convert seconds to milliseconds
                    }
                }
            } else if (uri.scheme == ContentResolver.SCHEME_FILE) {
                val file = uri.path?.let { java.io.File(it) }
                if (file?.exists() == true) {
                    return Date(file.lastModified())
                }
            }
        } catch (e: Exception) {
            Log.e("MediaUploadVM", "Error getting file last modified date for $uri", e)
        }
        return null
    }

    private fun getMimeTypeExtension(mimeType: String?): String? {
        return mimeType?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }?.let { ".$it" }
    }

    fun getBestDate(context: Context, uri: Uri): Date? {

        // 1. Try Exif (Best for Camera photos, usually null for WhatsApp)
        val exifDate = getExifDate(context, uri)
        if (exifDate != null) return exifDate

        // 2. Try the "last_modified" column (Specific to your URI type)
        val documentDate = getDocumentLastModified(context, uri)
        if (documentDate != null) return documentDate

        // 3. Fallback to Filename (Date only, no time)
        return parseWhatsAppFilename(context, uri)
    }

    private fun getExifDate(context: Context, uri: Uri): Date? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif = inputStream?.let { ExifInterface(it) }

        var dateCreatedString: String? = null
        var dateCreated: Date? = null

        val dateCreatedOriginal = exif?.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) // e.g., "2025:04:24 14:33:00"
        if (!dateCreatedOriginal.isNullOrBlank()) {
            dateCreatedString = dateCreatedOriginal
        } else {
            val dateModified = exif?.getAttribute(ExifInterface.TAG_DATETIME)
            if (!dateModified.isNullOrBlank()) {
                dateCreatedString = dateModified
            } else {
                val dateTaken = exif?.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                if (!dateTaken.isNullOrBlank()) {
                    dateCreatedString = dateTaken
                }
            }
        }

        if (!dateCreatedString.isNullOrBlank()) {
            dateCreated = parseExifDate(dateCreatedString)
        } else {
            val mediaStoreDate = getImageDateFromMediaStore(context, uri)
            if (mediaStoreDate != null) {
                dateCreated = parseExifDate(timestamp = mediaStoreDate)
            }
        }

        inputStream?.close()
        return dateCreated
    }

    private fun getDocumentLastModified(context: Context, uri: Uri): Date? {
        // The column name explicitly found in your logs
        val columnLastModified = "last_modified"

        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(columnLastModified)
                    if (index != -1) {
                        val lastModified = it.getLong(index)

                        // VALIDATION: Check if it's seconds or milliseconds
                        // If the number is small (e.g. 17xxxxx), it's seconds -> * 1000
                        // If the number is huge (e.g. 17xxxxxxxxx), it's millis -> Use as is
                        if (lastModified > 0) {
                            // Simple heuristic: Timestamp in millis for year 2000 is ~9.4e11
                            return if (lastModified < 10000000000L) {
                                Date(lastModified * 1000)
                            } else {
                                Date(lastModified)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.message?.let { Log.e("MediaUploadViewModel",it) }
            // Log error
        }
        return null
    }

    // --- Helper: Parse WhatsApp Filename ---
    // WhatsApp files are named like "IMG-20240214-WA0001.jpg"
    private fun parseWhatsAppFilename(context: Context, uri: Uri): Date? {
        try {
            // Get the filename from the URI
            var filename: String? = null
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    filename = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                }
            }

            if (filename != null && filename.startsWith("IMG-")) {
                // Regex to match YYYYMMDD
                val pattern = Pattern.compile("IMG-(\\d{8})-WA.*")
                val matcher = pattern.matcher(filename)
                if (matcher.find()) {
                    val dateString = matcher.group(1) ?: return null // Extracts "20240214"
                    return SimpleDateFormat("yyyyMMdd", Locale.US).parse(dateString)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getImageDateFromMediaStore(context: Context, uri: Uri): Long? {
        // Your existing getImageDateFromMediaStore logic...
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null // MediaStore only for content URIs

        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                var dateValue: Long?
                val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                if (dateTakenIndex != -1) {
                    dateValue = cursor.getLong(dateTakenIndex)
                    if (dateValue > 0) return dateValue // DATE_TAKEN is usually in ms
                }

                val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                if (dateModifiedIndex != -1) {
                    dateValue = cursor.getLong(dateModifiedIndex)
                    if (dateValue > 0) return dateValue * 1000 // DATE_MODIFIED is often in seconds
                }
            }
        }
        return null
    }

    private fun formatMediaName(date: Date?, ext: String): String {
        if (date == null) return ""

        return try {
            val outputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            date.let { outputFormat.format(it) }.plus(ext)
        } catch (_: Exception) {
            "" // Handle parsing errors
        }
    }
}
