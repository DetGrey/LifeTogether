package com.example.lifetogether.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.lifetogether.data.logic.rotateBasedOnExif
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.callback.TempFileDownloadResult
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.inject.Inject

class FirebaseStorageDataSource@Inject constructor() {
    // ------------------------------------------------------------------------------- IMAGES
    suspend fun uploadPhoto(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener {
        return try {
            println("FirebaseStorageDataSource uploadPhoto uri: $uri")

            var path: String
            var maxWidth = 600
            val maxHeight = 600
            var needsResize = true
            var ext = ".jpeg"

            when (imageType) {
                is ImageType.ProfileImage -> {
                    path = Constants.USER_TABLE
                }

                is ImageType.FamilyImage -> {
                    path = Constants.FAMILIES_TABLE
                    maxWidth = 1200
                }

                is ImageType.RecipeImage -> {
                    path = Constants.RECIPES_TABLE
                    maxWidth = 1200
                }

                is ImageType.GalleryMedia -> {
                    if (imageType.galleryMediaUploadData == null) {
                        return StringResultListener.Failure("GalleryMediaUploadData is null")
                    }
                    path = Constants.GALLERY_MEDIA_TABLE
                    needsResize = false
                    ext = imageType.galleryMediaUploadData.extension
                }
            }
            val correctedByteArray = uri.rotateBasedOnExif(context)
            if (correctedByteArray == null) {
                println("Failed to rotate image")
                return StringResultListener.Failure("Failed to rotate image")
            }

            // Resize image only if needed
            val byteArray = if (needsResize) {
                val resizedBitmap =
                    resizeImageWithCoil(context, correctedByteArray, maxWidth, maxHeight)

                ByteArrayOutputStream().apply {
                    resizedBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, this)
                }.toByteArray()
            } else {
                correctedByteArray
            }

            // Create a reference to Firebase Storage
            path += "/${UUID.randomUUID()}-${System.currentTimeMillis()}$ext"

            val photoRef = FirebaseStorage.getInstance().reference.child(path)

            // Upload the ByteArray to Firebase Storage
            photoRef.putBytes(byteArray).await()

            // Get the download URL
            val downloadUrl = photoRef.downloadUrl.await()
            println("FirebaseStorageDataSource uploadPhoto downloadUrl: $downloadUrl")
            StringResultListener.Success(downloadUrl.toString())
        } catch (e: Exception) {
            StringResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun fetchImageByteArray(url: String): ByteArrayResultListener {
        return try {
            println("FirebaseStorageDataSource downloadImage()")
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            val byteArray = storageRef.getBytes(Long.MAX_VALUE).await()
            ByteArrayResultListener.Success(byteArray)
        } catch (e: Exception) {
            ByteArrayResultListener.Failure("Error: ${e.message}")
        }
    }

    private suspend fun resizeImageWithCoil(
        context: Context,
        imageData: ByteArray,
        maxWidth: Int,
        maxHeight: Int,
    ): Bitmap? {
        return withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)

            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(bitmap) // Use decoded bitmap instead of URI
                .size(maxWidth, maxHeight)
                .build()

            val result = imageLoader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        }
    }

    suspend fun deleteImage(url: String): ResultListener {
        return try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            storageRef.delete().await()
            ResultListener.Success
        } catch (e: Exception) {
            ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- VIDEOS
    suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): StringResultListener {
        return try {
            Log.d("FirebaseStorageDS", "uploadVideo uri: $uri, pathId: $path, ext: $extension")

            // Create a unique file name and the full reference path in Firebase Storage
            // This is similar to: path += "/${UUID.randomUUID()}-${System.currentTimeMillis()}$ext"
            val fileName = "${UUID.randomUUID()}-${System.currentTimeMillis()}$extension"
            val fullStoragePath = "$path/$fileName"

            val videoRef = FirebaseStorage.getInstance().reference.child(fullStoragePath)

            // Upload the file directly from the Uri using putFile for efficiency
            videoRef.putFile(uri).await()

            // Get the download URL
            val downloadUrl = videoRef.downloadUrl.await()
            Log.d("FirebaseStorageDS", "uploadVideo success. Download URL: $downloadUrl")
            StringResultListener.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Log.e("FirebaseStorageDS", "Error uploading video: ${e.message}", e)
            StringResultListener.Failure("Error uploading video: ${e.message}")
        }
    }

    suspend fun downloadContentToTempFile(
        context: Context,
        storageUrl: String,
        desiredFileExtension: String,
    ): TempFileDownloadResult {
        // Create a unique temporary file in the app's cache directory
        val ensuredExtension = if (desiredFileExtension.startsWith(".")) desiredFileExtension else ".$desiredFileExtension"
        val tempFileName = "${UUID.randomUUID()}$ensuredExtension"

        return try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(storageUrl)

            val tempFile = File(context.cacheDir, tempFileName)

            // Await the completion of the file download
            storageRef.getFile(tempFile).await() // This suspends until download is complete or fails

            TempFileDownloadResult.Success(tempFile)
        } catch (e: Exception) {
            Log.e("FirebaseStorageDS", "Content download failure: ${e.message}", e)
            // Attempt to delete partially downloaded file on failure, if it exists
            val fileToDeleteOnFailure = File(context.cacheDir, tempFileName)
            if (fileToDeleteOnFailure.exists()) {
                if (!fileToDeleteOnFailure.delete()) {
                    Log.w("FirebaseStorageDS", "Failed to delete temp file on failure: ${fileToDeleteOnFailure.absolutePath}")
                }
            }
            TempFileDownloadResult.Failure("Download failed: ${e.message}")
        }
    }
}
