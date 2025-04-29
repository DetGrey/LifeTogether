package com.example.lifetogether.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

class FirebaseStorageDataSource@Inject constructor() {
    // ------------------------------------------------------------------------------- USERS
    suspend fun uploadPhoto(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener {
        return try {
            println("FirebaseStorageDataSource uploadPhoto uri: $uri")

            var path = ""
            var maxWidth = 600
            var maxHeight = 600
            var needsResize = true
            var type = ".jpg"

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
                is ImageType.GalleryImage -> {
                    path = Constants.GALLERY_IMAGES_TABLE
                    needsResize = false
                    type = ".png"
                }
            }

            // Resize image only if needed
            val byteArray = if (needsResize) {
                val resizedBitmap = resizeImageWithCoil(context, uri, maxWidth, maxHeight)

                ByteArrayOutputStream().apply {
                    resizedBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, this)
                }.toByteArray()
            } else {
                context.contentResolver.openInputStream(uri)?.readBytes() ?: ByteArray(0)
            }

            // Create a reference to Firebase Storage
            path += "/${UUID.randomUUID()}-${System.currentTimeMillis()}$type"

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

    suspend fun downloadImage(url: String): ByteArrayResultListener {
        return try {
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            val byteArray = storageRef.getBytes(Long.MAX_VALUE).await()
            ByteArrayResultListener.Success(byteArray)
        } catch (e: Exception) {
            ByteArrayResultListener.Failure("Error: ${e.message}")
        }
    }

    private suspend fun resizeImageWithCoil(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            val imageLoader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
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
}
