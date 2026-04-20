package com.example.lifetogether.domain.usecase.image

import android.content.ContentValues.TAG
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.gallery.MediaUploadData
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import javax.inject.Inject

class UploadGalleryMediaItemsUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val galleryRepository: GalleryRepository,
) {
    companion object {
        // Limit to 1 concurrent upload - AWS SDK buffers request bodies for checksums
        private const val MAX_CONCURRENT_UPLOADS = 1
        // Max 100MB per file - AWS SDK + Android device constraints
        private const val MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024L
    }

    suspend operator fun invoke(
        mediaUploadList: List<MediaUploadData>,
        context: Context,
    ): Result<Unit, String> {
        if (mediaUploadList.isEmpty()) {
            Log.d(TAG, "MediaUploadList is empty. Nothing to upload.")
            return Result.Success(Unit)
        }

        // Validate file sizes before uploading to prevent OOM
        val oversizedItems = mutableListOf<String>()
        for (mediaData in mediaUploadList) {
            try {
                val cursor = context.contentResolver.query(
                    mediaData.uri,
                    arrayOf(MediaStore.MediaColumns.SIZE),
                    null,
                    null,
                    null
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val fileSize = it.getLong(0)
                        if (fileSize > MAX_FILE_SIZE_BYTES) {
                            oversizedItems.add("${mediaData.mediaType.itemName} (${fileSize / 1024 / 1024}MB)")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not determine file size: ${e.message}")
            }
        }

        if (oversizedItems.isNotEmpty()) {
            return Result.Failure("Files too large (max 100MB each): ${oversizedItems.joinToString(", ")}")
        }

        val albumId = mediaUploadList.first().mediaType.albumId
        val familyId = mediaUploadList.first().mediaType.familyId // Also get familyId

        // Process uploads with limited concurrency to prevent OOM
        // This list will store successfully processed media items or null for failures
        val processedResults: List<Pair<GalleryMedia?, String?>> = coroutineScope {
            val semaphore = Semaphore(MAX_CONCURRENT_UPLOADS)
            val deferredTasks: List<Deferred<Pair<GalleryMedia?, String?>>> = mediaUploadList.map { mediaData ->
                async(Dispatchers.IO) { // Each async block will return Pair(GalleryMedia_on_success_OR_null, error_message_string_OR_null)
                    semaphore.acquire()
                    try {
                        val (mediaType, uri, extension) = Triple(mediaData.mediaType, mediaData.uri, mediaData.extension)
                        val itemName = mediaType.itemName
                        when (mediaData) {
                            is MediaUploadData.ImageUpload -> {
                                Log.d(TAG, "Uploading image: $uri for item: $itemName")
                                val imageType = ImageType.GalleryMedia(
                                    familyId,
                                    albumId,
                                    MediaUploadData.ImageUpload(
                                        uri,
                                        mediaType as GalleryImage,
                                        extension,
                                    ),
                                )

                                val fileUploadResult = imageRepository.uploadImage(
                                    uri,
                                    imageType,
                                    context,
                                )

                                when (fileUploadResult) {
                                    is Result.Success -> {
                                        Log.d(TAG, "Image $itemName uploaded: ${fileUploadResult.data}")
                                        Pair(mediaType.copy(mediaUrl = fileUploadResult.data), null)
                                    }

                                    is Result.Failure -> {
                                        Log.e(TAG, "Failed to upload image $itemName: ${fileUploadResult.error}")
                                        Pair(null, "Image $itemName: ${fileUploadResult.error}")
                                    }
                                }
                            }

                            is MediaUploadData.VideoUpload -> {
                                Log.d(TAG, "Uploading video: $uri for item: $itemName")

                                val fileUploadResult = galleryRepository.uploadVideo(
                                    uri,
                                    Constants.GALLERY_MEDIA_TABLE,
                                    extension,
                                )

                                when (fileUploadResult) {
                                    is Result.Success -> {
                                        Log.d(TAG, "Video $itemName uploaded: ${fileUploadResult.data}")
                                        Pair((mediaType as GalleryVideo).copy(mediaUrl = fileUploadResult.data), null)
                                    }

                                    is Result.Failure -> {
                                        Log.e(TAG, "Failed to upload video $itemName: ${fileUploadResult.error}")
                                        Pair(null, "Video $itemName: ${fileUploadResult.error}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during upload for ${mediaData.mediaType.itemName} (${mediaData.uri}): ${e.message}", e)
                        Pair(null, "Exception for ${mediaData.mediaType.itemName}: ${e.message}")
                    } finally {
                        semaphore.release()
                    }
                }
            }
            deferredTasks.awaitAll()
        }

        val successfullyUploadedMedia = processedResults.mapNotNull { it.first }
        val uploadErrorMessages = processedResults.mapNotNull { it.second }

        if (successfullyUploadedMedia.isEmpty() && mediaUploadList.isNotEmpty()) {
            val combinedError = uploadErrorMessages.joinToString(separator = "\n")
            Log.e(TAG, "All media items failed to upload. Errors: $combinedError")
            return Result.Failure("All media uploads failed.${if (combinedError.isNotBlank()) " Details: $combinedError" else ""}".take(250))
        }

        if (uploadErrorMessages.isNotEmpty()) {
            Log.w(TAG, "Some media items failed to upload: ${uploadErrorMessages.joinToString()}")
        }

        if (successfullyUploadedMedia.isNotEmpty()) {
            Log.d(TAG, "Saving metadata for ${successfullyUploadedMedia.size} items.")
            val saveMetaDataResult = galleryRepository.saveGalleryMediaMetaData(successfullyUploadedMedia)

            return if (saveMetaDataResult is Result.Success) {
                    Log.d(TAG, "Metadata saved successfully. Updating album count.")
                    galleryRepository.updateAlbumCount(albumId, successfullyUploadedMedia.size).let { countResult ->
                        if (countResult is Result.Failure) {
                            Log.w(TAG, "Failed to update album count: ${countResult.error}")
                        }
                    }
                    if (uploadErrorMessages.isNotEmpty()) {
                        Result.Failure("Partial success. ${uploadErrorMessages.size} item(s) failed: ${uploadErrorMessages.joinToString(", ").take(200)}")
                    } else {
                        Result.Success(Unit)
                    }
            } else if (saveMetaDataResult is Result.Failure) {
                Log.e(TAG, "Failed to save metadata: ${saveMetaDataResult.error}")
                Result.Failure("Failed to save media metadata: ${saveMetaDataResult.error}")
            } else {
                Log.e(TAG, "Unknown result from saveMediaMetaData.")
                Result.Failure("Unknown error saving metadata.")
            }
        }

        Log.d(TAG, "No media items were successfully uploaded or prepared for metadata.")
        return Result.Failure("No media items processed successfully.")
    }
}
