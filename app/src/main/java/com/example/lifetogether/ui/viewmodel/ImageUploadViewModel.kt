package com.example.lifetogether.ui.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.logic.parseExifDate
import com.example.lifetogether.domain.logic.toBitmap
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.model.sealed.UploadState
import com.example.lifetogether.domain.usecase.image.UploadImageUseCase
import com.example.lifetogether.domain.usecase.image.UploadImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ImageUploadViewModel @Inject constructor(
    private val uploadImageUseCase: UploadImageUseCase,
    private val uploadImagesUseCase: UploadImagesUseCase,
) : ViewModel() {
    var error: String by mutableStateOf("")

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun resetViewModel() {
        _imageUri.value = null
        _bitmap.value = null
        _imageUris.value = emptyList()
        _uploadState.value = UploadState.Idle
    }

    // ---------------------------------------------------------------- SINGLE IMAGE
    private val _imageUri = MutableStateFlow<Uri?>(null)
    private val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    fun setImageUri(uri: Uri, contentResolver: ContentResolver) {
        _imageUri.value = uri
        _bitmap.value = uri.toBitmap(contentResolver)
    }

    fun uploadPhoto(
        imageType: ImageType,
        context: Context,
    ) {
        val uri = imageUri.value ?: return
        _uploadState.value = UploadState.Uploading

        viewModelScope.launch {
            val result = uploadImageUseCase.invoke(uri, imageType, context)
            when (result) {
                is ResultListener.Success -> {
                    _uploadState.value = UploadState.Success
                }
                is ResultListener.Failure -> _uploadState.value = UploadState.Failure(result.message)
            }
        }
    }

    // ---------------------------------------------------------------- MULTIPLE IMAGES
    private val _imageUris = MutableStateFlow<List<Uri>>(emptyList())
    val imageUris: StateFlow<List<Uri>> = _imageUris.asStateFlow()

    fun setImageUris(uris: List<Uri>) {
        _imageUris.value = uris
    }

    fun uploadPhotos(imageType: ImageType, context: Context) {
        if (imageUris.value.isEmpty()) return
        _uploadState.value = UploadState.Uploading

        val images = when (imageType) {
            is ImageType.GalleryImage -> {
                imageUris.value.map { uri ->
                    val exifData = extractImageMetadata(context, uri)
                    Pair(uri, GalleryImage(
                        familyId = imageType.familyId,
                        itemName = exifData.itemName,
                        albumId = imageType.albumId,
                        dateCreated = exifData.dateCreated
                    ))

                }
            }
            else -> {
                _uploadState.value = UploadState.Failure("Invalid image type")
                return
            }
        }

        println("Images: $images")
        val imageTypeUpdated = imageType.copy(galleryImages = images)

        viewModelScope.launch {
            when (val results = uploadImagesUseCase.invoke(imageUris.value, imageTypeUpdated, context)) {
                is ResultListener.Success -> _uploadState.value = UploadState.Success
                is ResultListener.Failure -> _uploadState.value = UploadState.Failure(results.message)
            }
        }
    }

    data class ExifData(
        val uri: Uri,
        val itemName: String,
        val dateCreated: Date?,
    )

    private fun extractImageMetadata(context: Context, uri: Uri): ExifData {
        // Get the date and name metadata
        val dateCreated = getExifDate(context, uri)
        val itemName = formatExifDateImageName(dateCreated) ?: ""

        return ExifData(uri, itemName, dateCreated)
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

    private fun getImageDateFromMediaStore(context: Context, uri: Uri): Long? {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                return if (dateIndex != -1) cursor.getLong(dateIndex) else null
            }
        }
        return null // No date found
    }

    private fun formatExifDateImageName(date: Date?): String? {
        if (date == null) return null

        return try {
            val outputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            date.let { outputFormat.format(it) }
        } catch (e: Exception) {
            null // Handle parsing errors
        }
    }
}
