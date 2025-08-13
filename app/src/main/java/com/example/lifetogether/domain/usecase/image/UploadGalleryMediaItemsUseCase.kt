package com.example.lifetogether.domain.usecase.image

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import com.example.lifetogether.data.repository.RemoteImageRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.gallery.MediaUploadData
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class UploadGalleryMediaItemsUseCase @Inject constructor(
    private val remoteImageRepository: RemoteImageRepositoryImpl,
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        mediaUploadList: List<MediaUploadData>,
        context: Context,
    ): ResultListener {
        if (mediaUploadList.isEmpty()) {
            Log.d(TAG, "MediaUploadList is empty. Nothing to upload.")
            return ResultListener.Success
        }

        val albumId = mediaUploadList.first().mediaType.albumId
        val familyId = mediaUploadList.first().mediaType.familyId // Also get familyId

        // Process uploads concurrently for better performance
        // This list will store successfully processed media items or null for failures
        val processedResults: List<Pair<GalleryMedia?, String?>> = coroutineScope {
            val deferredTasks: List<Deferred<Pair<GalleryMedia?, String?>>> = mediaUploadList.map { mediaData ->
                async { // Each async block will return Pair(GalleryMedia_on_success_OR_null, error_message_string_OR_null)
                    val (mediaType, uri, extension) = Triple(mediaData.mediaType, mediaData.uri, mediaData.extension)
                    val itemName = mediaType.itemName

                    try {
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

                                val fileUploadResult = remoteImageRepository.uploadImage(
                                    uri,
                                    imageType,
                                    context,
                                )

                                when (fileUploadResult) {
                                    is StringResultListener.Success -> {
                                        Log.d(TAG, "Image $itemName uploaded: ${fileUploadResult.string}")
                                        Pair(mediaType.copy(mediaUrl = fileUploadResult.string), null)
                                    }

                                    is StringResultListener.Failure -> {
                                        Log.e(TAG, "Failed to upload image $itemName: ${fileUploadResult.message}")
                                        Pair(null, "Image $itemName: ${fileUploadResult.message}")
                                    }
                                }
                            }

                            is MediaUploadData.VideoUpload -> {
                                Log.d(TAG, "Uploading video: $uri for item: $itemName")

                                val fileUploadResult = remoteImageRepository.uploadVideo(
                                    uri,
                                    Constants.GALLERY_MEDIA_TABLE,
                                    extension,
                                )

                                when (fileUploadResult) {
                                    is StringResultListener.Success -> {
                                        Log.d(TAG, "Video $itemName uploaded: ${fileUploadResult.string}")
                                        Pair((mediaType as GalleryVideo).copy(mediaUrl = fileUploadResult.string), null)
                                    }

                                    is StringResultListener.Failure -> {
                                        Log.e(TAG, "Failed to upload video $itemName: ${fileUploadResult.message}")
                                        Pair(null, "Video $itemName: ${fileUploadResult.message}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during upload for $itemName ($uri): ${e.message}", e)
                        Pair(null, "Exception for $itemName: ${e.message}")
                    }
                } as Deferred<Pair<GalleryMedia?, String?>>
            }
            deferredTasks.awaitAll()
        }

        val successfullyUploadedMedia = processedResults.mapNotNull { it.first }
        val uploadErrorMessages = processedResults.mapNotNull { it.second }

        if (successfullyUploadedMedia.isEmpty() && mediaUploadList.isNotEmpty()) {
            val combinedError = uploadErrorMessages.joinToString(separator = "\n")
            Log.e(TAG, "All media items failed to upload. Errors: $combinedError")
            return ResultListener.Failure("All media uploads failed.${if (combinedError.isNotBlank()) " Details: $combinedError" else ""}".take(250))
        }

        if (uploadErrorMessages.isNotEmpty()) {
            Log.w(TAG, "Some media items failed to upload: ${uploadErrorMessages.joinToString()}")
        }

        if (successfullyUploadedMedia.isNotEmpty()) {
            Log.d(TAG, "Saving metadata for ${successfullyUploadedMedia.size} items.")
            val saveMetaDataResult = remoteImageRepository.saveGalleryMediaMetaData(successfullyUploadedMedia)

            return if (saveMetaDataResult is ResultListener.Success) {
                Log.d(TAG, "Metadata saved successfully. Updating album count.")
                remoteListRepository.updateAlbumCount(albumId, successfullyUploadedMedia.size).let { countResult ->
                    if (countResult is ResultListener.Failure) {
                        Log.w(TAG, "Failed to update album count: ${countResult.message}")
                    }
                }
                if (uploadErrorMessages.isNotEmpty()) {
                    ResultListener.Failure("Partial success. ${uploadErrorMessages.size} item(s) failed: ${uploadErrorMessages.joinToString(", ").take(200)}")
                } else {
                    ResultListener.Success
                }
            } else if (saveMetaDataResult is ResultListener.Failure) {
                Log.e(TAG, "Failed to save metadata: ${saveMetaDataResult.message}")
                ResultListener.Failure("Failed to save media metadata: ${saveMetaDataResult.message}")
            } else {
                Log.e(TAG, "Unknown result from saveMediaMetaData.")
                ResultListener.Failure("Unknown error saving metadata.")
            }
        }

        Log.d(TAG, "No media items were successfully uploaded or prepared for metadata.")
        return ResultListener.Failure("No media items processed successfully.")
    }
}
