package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.toByteArray
import aws.smithy.kotlin.runtime.content.writeToFile
import aws.smithy.kotlin.runtime.net.url.Url
import com.example.lifetogether.BuildConfig
import com.example.lifetogether.data.logic.ImageProcessor
import com.example.lifetogether.di.IoDispatcher
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.datasource.StorageDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import androidx.core.net.toUri

class CloudflareR2StorageDataSource @Inject constructor(
    private val application: Application,
    private val imageProcessor: ImageProcessor,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : StorageDataSource {

    companion object {
        // Configuration loaded from BuildConfig (which reads from local.properties)
        private const val ACCOUNT_ID = BuildConfig.R2_ACCOUNT_ID
        private const val BUCKET_NAME = BuildConfig.R2_BUCKET_NAME
        private const val ACCESS_KEY_ID = BuildConfig.R2_ACCESS_KEY_ID
        private const val SECRET_ACCESS_KEY = BuildConfig.R2_SECRET_ACCESS_KEY
        private const val PUBLIC_URL_BASE = BuildConfig.R2_PUBLIC_DOMAIN
        
        private const val ENDPOINT = "https://$ACCOUNT_ID.r2.cloudflarestorage.com"
        
        private const val TAG = "CloudflareR2Storage"
    }

    private val s3Client by lazy {
        val credentials = Credentials(
            accessKeyId = ACCESS_KEY_ID,
            secretAccessKey = SECRET_ACCESS_KEY
        )
        
        S3Client {
            region = "auto"
            endpointUrl = Url.parse(ENDPOINT)
            credentialsProvider = StaticCredentialsProvider(credentials)
            retryStrategy {
                maxAttempts = 1  // Minimize retries to reduce memory usage
            }
        }
    }

    // ------------------------------------------------------------------------------- IMAGES
    override suspend fun uploadPhoto(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<String, AppError> {
        return try {
            // Process image (rotate, resize, compress)
            val processedImage = imageProcessor.processImage(uri, imageType, context)
                ?: return Result.Failure(AppErrors.validation("Failed to process image")).also {
                    Log.e(TAG, "uploadPhoto processing failed")
                }

            // Create object key (path) in R2
            val objectKey = "${processedImage.path}/${UUID.randomUUID()}-${System.currentTimeMillis()}${processedImage.extension}"

            // Upload to R2 using S3 SDK
            val putObjectRequest = PutObjectRequest {
                bucket = BUCKET_NAME
                key = objectKey
                body = processedImage.data.asByteStream()
                contentType = when {
                    processedImage.extension.contains("jpeg") || processedImage.extension.contains("jpg") -> "image/jpeg"
                    processedImage.extension.contains("png") -> "image/png"
                    processedImage.extension.contains("mp4") -> "video/mp4"
                    else -> "application/octet-stream"
                }
            }
            s3Client.putObject(putObjectRequest)

            // Return the R2.dev public URL
            val downloadUrl = "$PUBLIC_URL_BASE/$objectKey"
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading photo", e)
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    override suspend fun fetchImageByteArray(url: String): Result<ByteArray, AppError> {
        return try {
            val objectKey = extractObjectKeyFromUrl(url)

            val getObjectRequest = GetObjectRequest {
                bucket = BUCKET_NAME
                key = objectKey
            }
            val response = s3Client.getObject(getObjectRequest) { resp ->
                resp.body?.toByteArray()
            }
            Result.Success(response ?: byteArrayOf())
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching image", e)
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    override suspend fun deleteImage(url: String): Result<Unit, AppError> {
        return try {
            val objectKey = extractObjectKeyFromUrl(url)

            val deleteObjectRequest = DeleteObjectRequest {
                bucket = BUCKET_NAME
                key = objectKey
            }
            s3Client.deleteObject(deleteObjectRequest)

            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting image", e)
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    override suspend fun deleteImages(urlList: List<String>): Result<Unit, AppError> =
        coroutineScope {
            if (urlList.isEmpty()) {
                Log.i(TAG, "deleteImages: URL list is empty. Nothing to delete.")
                return@coroutineScope Result.Success(Unit)
            }

            val deferredResults = urlList.map { url ->
                async(ioDispatcher) {
                    deleteImage(url)
                }
            }

            val results = deferredResults.awaitAll()

            val allSucceeded = results.all { it is Result.Success }

            if (allSucceeded) {
                Log.i(TAG, "Successfully deleted all ${urlList.size} images.")
                Result.Success(Unit)
            } else {
                val failedCount = results.count { it is Result.Failure }
                val firstErrorMessage = (results.firstOrNull { it is Result.Failure } as? Result.Failure)?.error
                    ?: "One or more images failed to delete."
                Log.e(TAG, "$failedCount image(s) failed to delete. First error: $firstErrorMessage")
                Result.Failure(AppErrors.storage("$failedCount image(s) failed to delete. First error: $firstErrorMessage"))
            }
        }

    // ------------------------------------------------------------------------------- VIDEOS
    override suspend fun uploadVideo(
        uri: Uri,
        path: String,
        extension: String,
    ): Result<String, AppError> {
        return try {
            val fileName = "${UUID.randomUUID()}-${System.currentTimeMillis()}$extension"
            val objectKey = "$path/$fileName"

            // Open and use input stream, ensuring it's closed after upload
            withContext(ioDispatcher) {
                application.contentResolver.openInputStream(uri)?.use { videoStream ->
                    // Stream video upload to avoid loading entire file into memory
                    val putObjectRequest = PutObjectRequest {
                        bucket = BUCKET_NAME
                        key = objectKey
                        body = videoStream.asByteStream()
                        contentType = "video/mp4"
                    }
                    s3Client.putObject(putObjectRequest)
                } ?: throw IllegalStateException("Failed to open video stream from Uri")
            }

            val downloadUrl = "$PUBLIC_URL_BASE/$objectKey"
            
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading video", e)
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    override suspend fun downloadContentToTempFile(
        context: Context,
        storageUrl: String,
        desiredFileExtension: String,
    ): Result<File, AppError> {
        val ensuredExtension = if (desiredFileExtension.startsWith(".")) desiredFileExtension else ".$desiredFileExtension"
        val tempFileName = "${UUID.randomUUID()}$ensuredExtension"

        return try {
            val objectKey = extractObjectKeyFromUrl(storageUrl)
            val tempFile = File(context.cacheDir, tempFileName)

            val getObjectRequest = GetObjectRequest {
                bucket = BUCKET_NAME
                key = objectKey
            }
            s3Client.getObject(getObjectRequest) { resp ->
                // Stream directly to disk to avoid loading large objects into memory
                resp.body?.writeToFile(tempFile)
            }

            Result.Success(tempFile)
        } catch (e: Exception) {
            Log.e(TAG, "Content download failure", e)
            val fileToDeleteOnFailure = File(context.cacheDir, tempFileName)
            if (fileToDeleteOnFailure.exists()) {
                if (!fileToDeleteOnFailure.delete()) {
                    Log.w(TAG, "Failed to delete temp file on failure: ${fileToDeleteOnFailure.absolutePath}")
                }
            }
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    // ------------------------------------------------------------------------------- HELPERS

    /**
     * Extract the R2 object key from an R2.dev public URL.
     * Example: "https://pub-abc123.r2.dev/users/abc-123.jpeg" -> "users/abc-123.jpeg"
     */
    private fun extractObjectKeyFromUrl(url: String): String {
        return try {
            val uri = url.toUri()
            // Remove leading slash from path to get the object key
            uri.path?.removePrefix("/") ?: url
        } catch (_: Exception) {
            Log.w(TAG, "Failed to parse URL: $url, using as-is")
            url
        }
    }
}
