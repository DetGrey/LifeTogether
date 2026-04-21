package com.example.lifetogether.data.logic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.lifetogether.di.IoDispatcher
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles image processing operations like rotation correction, resizing, and compression.
 * Extracted from data sources to follow Single Responsibility Principle.
 */
@Singleton
class ImageProcessor @Inject constructor(
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    companion object {
        private const val TAG = "ImageProcessor"
        private const val DEFAULT_MAX_WIDTH = 600
        private const val DEFAULT_MAX_HEIGHT = 600
        private const val LARGE_IMAGE_WIDTH = 1200
        private const val JPEG_QUALITY = 70
    }

    /**
     * Process an image: rotate based on EXIF, resize if needed, and compress.
     * Returns the processed byte array ready for upload.
     */
    suspend fun processImage(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): ProcessedImage? {
        return withContext(ioDispatcher) {
            try {
                // Get path and processing parameters based on image type
                val config = getImageConfig(imageType) ?: return@withContext null

                // Perform EXIF rotation correction
                val correctedByteArray = uri.rotateBasedOnExif(context)
                if (correctedByteArray == null) {
                    Log.e(TAG, "Failed to rotate image for type: $imageType")
                    return@withContext null
                }

                // Resize image only if needed
                val finalByteArray = if (config.needsResize) {
                    val resizedBitmap = resizeImageWithCoil(
                        context,
                        correctedByteArray,
                        config.maxWidth,
                        config.maxHeight
                    )

                    if (resizedBitmap == null) {
                        Log.e(TAG, "Image resize failed")
                        return@withContext null
                    }

                    ByteArrayOutputStream().apply {
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, this)
                    }.toByteArray()
                } else {
                    correctedByteArray
                }

                ProcessedImage(
                    data = finalByteArray,
                    path = config.path,
                    extension = config.extension
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Calculate inSampleSize to avoid loading huge images into memory.
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Resize image using Coil image loader with specified dimensions.
     */
    private suspend fun resizeImageWithCoil(
        context: Context,
        imageData: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap? {
        return withContext(ioDispatcher) {
            try {
                // First decode bounds to calculate inSampleSize
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
                
                // Calculate inSampleSize to avoid OOM on very large images
                options.inSampleSize = calculateInSampleSize(options, maxWidth * 2, maxHeight * 2)
                options.inJustDecodeBounds = false
                
                val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
                    ?: return@withContext null

                val imageLoader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(bitmap)
                    .size(maxWidth, maxHeight)
                    .build()

                val result = imageLoader.execute(request)
                (result.drawable as? BitmapDrawable)?.bitmap
            } catch (e: Exception) {
                Log.e(TAG, "Error resizing image: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Get image configuration based on image type.
     */
    private fun getImageConfig(imageType: ImageType): ImageConfig? {
        return when (imageType) {
            is ImageType.ProfileImage -> ImageConfig(
                path = Constants.USER_TABLE,
                maxWidth = DEFAULT_MAX_WIDTH,
                maxHeight = DEFAULT_MAX_HEIGHT,
                needsResize = true,
                extension = ".jpeg"
            )

            is ImageType.FamilyImage -> ImageConfig(
                path = Constants.FAMILIES_TABLE,
                maxWidth = LARGE_IMAGE_WIDTH,
                maxHeight = DEFAULT_MAX_HEIGHT,
                needsResize = true,
                extension = ".jpeg"
            )

            is ImageType.RecipeImage -> ImageConfig(
                path = Constants.RECIPES_TABLE,
                maxWidth = LARGE_IMAGE_WIDTH,
                maxHeight = DEFAULT_MAX_HEIGHT,
                needsResize = true,
                extension = ".jpeg"
            )

            is ImageType.GalleryMedia -> {
                imageType.galleryMediaUploadData?.let { uploadData ->
                    ImageConfig(
                        path = Constants.GALLERY_MEDIA_TABLE,
                        maxWidth = DEFAULT_MAX_WIDTH,
                        maxHeight = DEFAULT_MAX_HEIGHT,
                        needsResize = false,
                        extension = uploadData.extension
                    )
                }
            }

            is ImageType.RoutineListEntryImage -> ImageConfig(
                path = Constants.ROUTINE_LIST_ENTRIES_TABLE,
                maxWidth = LARGE_IMAGE_WIDTH,
                maxHeight = DEFAULT_MAX_HEIGHT,
                needsResize = true,
                extension = ".jpeg"
            )
        }
    }

    /**
     * Configuration for image processing.
     */
    private data class ImageConfig(
        val path: String,
        val maxWidth: Int,
        val maxHeight: Int,
        val needsResize: Boolean,
        val extension: String,
    )
}

/**
 * Result of image processing containing the processed data and metadata.
 */
data class ProcessedImage(
    val data: ByteArray,
    val path: String,
    val extension: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProcessedImage

        if (!data.contentEquals(other.data)) return false
        if (path != other.path) return false
        if (extension != other.extension) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + extension.hashCode()
        return result
    }
}
