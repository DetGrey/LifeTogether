package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.lifetogether.data.logic.ImageProcessor
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject

class FirebaseStorageDataSource @Inject constructor(
    private val imageProcessor: ImageProcessor,
) : StorageDataSource {
    // ------------------------------------------------------------------------------- IMAGES
    override suspend fun uploadPhoto(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<String, AppError> {
        return appResultOfSuspend {
            Log.d("FirebaseStorageDS", "uploadPhoto uri: $uri")

            // Process image (rotate, resize, compress)
            val processedImage = imageProcessor.processImage(uri, imageType, context)
                ?: throw AppErrorThrowable(AppErrors.validation("Failed to process image"))

            // Create path for Firebase Storage
            val path = "${processedImage.path}/${UUID.randomUUID()}-${System.currentTimeMillis()}${processedImage.extension}"

            val photoRef = FirebaseStorage.getInstance().reference.child(path)

            // Upload the ByteArray to Firebase Storage
            photoRef.putBytes(processedImage.data).await()

            // Get the download URL
            val downloadUrl = photoRef.downloadUrl.await()
            Log.d("FirebaseStorageDS", "uploadPhoto success. Download URL: $downloadUrl")
            downloadUrl.toString()
        }
    }

    override suspend fun fetchImageByteArray(url: String): Result<ByteArray, AppError> {
        return appResultOfSuspend {
            Log.d("FirebaseStorageDS", "fetchImageByteArray from URL: $url")
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            storageRef.getBytes(Long.MAX_VALUE).await()
        }
    }

    override suspend fun deleteImage(url: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            Log.d("FirebaseStorageDS", "deleteImage: $url")
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            storageRef.delete().await()
            Log.d("FirebaseStorageDS", "deleteImage success")
        }
    }

    override suspend fun deleteImages(urlList: List<String>): Result<Unit, AppError> =
        coroutineScope {
            Result.Failure(AppErrors.unknown("Not implemented"))
        }

    // ------------------------------------------------------------------------------- VIDEOS
    override suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): Result<String, AppError> {
        return appResultOfSuspend {
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
            downloadUrl.toString()
        }
    }

    override suspend fun downloadContentToTempFile(
        context: Context,
        storageUrl: String,
        desiredFileExtension: String,
    ): Result<File, AppError> {
        // Create a unique temporary file in the app's cache directory
        val ensuredExtension = if (desiredFileExtension.startsWith(".")) desiredFileExtension else ".$desiredFileExtension"
        val tempFileName = "${UUID.randomUUID()}$ensuredExtension"

        return try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(storageUrl)

            val tempFile = File(context.cacheDir, tempFileName)

            // Await the completion of the file download
            storageRef.getFile(tempFile).await() // This suspends until download is complete or fails

            Result.Success(tempFile)
        } catch (e: Exception) {
            // Attempt to delete partially downloaded file on failure, if it exists
            val fileToDeleteOnFailure = File(context.cacheDir, tempFileName)
            if (fileToDeleteOnFailure.exists()) {
                if (!fileToDeleteOnFailure.delete()) {
                    Log.w("FirebaseStorageDS", "Failed to delete temp file on failure: ${fileToDeleteOnFailure.absolutePath}")
                }
            }
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }
}
