package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.content.ContentValues.TAG
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.lifetogether.di.IoDispatcher
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.gallery.MediaUploadData
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UploadGalleryMediaItemsUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    private val galleryRepository: GalleryRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        // Max 100MB per file - AWS SDK + Android device constraints
        private const val MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024L
    }

    suspend operator fun invoke(
        mediaUploadList: List<MediaUploadData>,
        context: Context,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<Unit, AppError> {
        return withContext(ioDispatcher) {
            if (mediaUploadList.isEmpty()) {
                Log.d(TAG, "MediaUploadList is empty. Nothing to upload.")
                return@withContext Result.Success(Unit)
            }

            val oversizedItems = mutableListOf<String>()
            for (mediaData in mediaUploadList) {
                try {
                    val cursor = context.contentResolver.query(
                        mediaData.uri,
                        arrayOf(MediaStore.MediaColumns.SIZE),
                        null,
                        null,
                        null,
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
                return@withContext Result.Failure(AppErrors.validation("Files too large (max 100MB each): ${oversizedItems.joinToString(", ")}"))
            }

            val albumId = mediaUploadList.first().mediaType.albumId
            val familyId = mediaUploadList.first().mediaType.familyId
            val successfullyUploadedMedia = mutableListOf<GalleryMedia>()
            val uploadErrorMessages = mutableListOf<String>()

            mediaUploadList.forEachIndexed { index, mediaData ->
                onProgress(index + 1, mediaUploadList.size)
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

                        when (val fileUploadResult = imageRepository.uploadImage(uri, imageType, context)) {
                            is Result.Success -> {
                                Log.d(TAG, "Image $itemName uploaded: ${fileUploadResult.data}")
                                successfullyUploadedMedia.add(mediaType.copy(mediaUrl = fileUploadResult.data))
                            }

                            is Result.Failure -> {
                                Log.e(TAG, "Failed to upload image $itemName: ${fileUploadResult.error}")
                                uploadErrorMessages.add("Image $itemName: ${fileUploadResult.error}")
                            }
                        }
                    }

                    is MediaUploadData.VideoUpload -> {
                        Log.d(TAG, "Uploading video: $uri for item: $itemName")

                        when (val fileUploadResult = galleryRepository.uploadVideo(uri, Constants.GALLERY_MEDIA_TABLE, extension)) {
                            is Result.Success -> {
                                Log.d(TAG, "Video $itemName uploaded: ${fileUploadResult.data}")
                                successfullyUploadedMedia.add((mediaType as GalleryVideo).copy(mediaUrl = fileUploadResult.data))
                            }

                            is Result.Failure -> {
                                Log.e(TAG, "Failed to upload video $itemName: ${fileUploadResult.error}")
                                uploadErrorMessages.add("Video $itemName: ${fileUploadResult.error}")
                            }
                        }
                    }
                }
            }

            if (successfullyUploadedMedia.isEmpty()) {
                val combinedError = uploadErrorMessages.joinToString(separator = "\n")
                Log.e(TAG, "All media items failed to upload. Errors: $combinedError")
                return@withContext Result.Failure(AppErrors.storage("All media uploads failed.${if (combinedError.isNotBlank()) " Details: $combinedError" else ""}".take(250)))
            }

            if (uploadErrorMessages.isNotEmpty()) {
                Log.w(TAG, "Some media items failed to upload: ${uploadErrorMessages.joinToString()}")
            }

            Log.d(TAG, "Saving metadata for ${successfullyUploadedMedia.size} items.")
            val saveMetaDataResult = galleryRepository.saveGalleryMediaMetaData(successfullyUploadedMedia)

            return@withContext if (saveMetaDataResult is Result.Success) {
                Log.d(TAG, "Metadata saved successfully. Updating album count.")
                galleryRepository.updateAlbumCount(albumId, successfullyUploadedMedia.size).let { countResult ->
                    if (countResult is Result.Failure) {
                        Log.w(TAG, "Failed to update album count: ${countResult.error}")
                    }
                }
                if (uploadErrorMessages.isNotEmpty()) {
                    Result.Failure(AppErrors.storage("Partial success. ${uploadErrorMessages.size} item(s) failed: ${uploadErrorMessages.joinToString(", ").take(200)}"))
                } else {
                    Result.Success(Unit)
                }
            } else if (saveMetaDataResult is Result.Failure) {
                Log.e(TAG, "Failed to save metadata: ${saveMetaDataResult.error}")
                Result.Failure(AppErrors.storage("Failed to save media metadata: ${saveMetaDataResult.error}"))
            } else {
                Log.e(TAG, "Unknown result from saveMediaMetaData.")
                Result.Failure(AppErrors.unknown("Unknown error saving metadata."))
            }
        }
    }
}
