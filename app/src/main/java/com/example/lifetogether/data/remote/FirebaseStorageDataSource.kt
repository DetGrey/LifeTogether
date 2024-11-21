package com.example.lifetogether.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
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

            // Resize the image using Coil
            val resizedBitmap = resizeImageWithCoil(context, uri, 500, 500)

            // Convert the resized Bitmap to ByteArray
            val stream = ByteArrayOutputStream()
            resizedBitmap?.compress(Bitmap.CompressFormat.PNG, 80, stream)
            val byteArray = stream.toByteArray()

            // Create a reference to Firebase Storage
            val path: String = when (imageType) {
                is ImageType.ProfileImage -> "profile"
                is ImageType.FamilyImage -> "family"
                is ImageType.RecipeImage -> "recipe"
            } + "/${UUID.randomUUID()}.jpg"

            val photoRef = FirebaseStorage.getInstance().reference.child(path)

            // Upload the ByteArray to Firebase Storage
            val uploadTask = photoRef.putBytes(byteArray).await()

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
}
