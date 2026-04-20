package com.example.lifetogether.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val userLocalDataSource: UserLocalDataSource,
    private val recipeLocalDataSource: RecipeLocalDataSource,
    private val userListLocalDataSource: UserListLocalDataSource,
    private val storageDataSource: StorageDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) {
    companion object {
        private const val TAG = "LocalImageRepositoryImpl"
    }
    fun getImageByteArray(imageType: ImageType): Flow<Result<ByteArray, String>> {
        Log.d(TAG, "getImageByteArray")
        val byteArrayFlow = when (imageType) {
            is ImageType.ProfileImage -> userLocalDataSource.getProfileImageByteArray(imageType.uid)
            is ImageType.FamilyImage -> userLocalDataSource.getFamilyImageByteArray(imageType.familyId)
            is ImageType.RecipeImage -> recipeLocalDataSource.getImageByteArray(
                familyId = imageType.familyId,
                recipeId = imageType.recipeId,
            )
            is ImageType.RoutineListEntryImage -> userListLocalDataSource.observeRoutineImageByteArray(imageType.entryId)
            is ImageType.GalleryMedia -> flowOf(null)
        }
        return byteArrayFlow.map { byteArray ->
            try {
                if (byteArray != null) {
                    Result.Success(byteArray)
                } else {
                    Result.Failure("No ByteArray found")
                }
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown error")
            }
        }
    }

    // ------------------- REMOTE
    suspend fun uploadImage(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): StringResultListener {
        return storageDataSource.uploadPhoto(uri, imageType, context)
    }

    suspend fun deleteImage(
        imageType: ImageType,
    ): ResultListener {
        return when (val urlResult = firestoreDataSource.getImageUrl(imageType)) {
            is StringResultListener.Success -> {
                storageDataSource.deleteImage(urlResult.string)
            }

            is StringResultListener.Failure -> {
                ResultListener.Failure(urlResult.message)
            }

            null -> ResultListener.Success // Means that there is no image to delete
        }
    }

    suspend fun deleteMediaFiles(
        urlList: List<String>,
    ): Result<Unit, String> {
        return storageDataSource.deleteImages(urlList)
    }

    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): ResultListener {
        return firestoreDataSource.saveImageDownloadUrl(url, imageType)
    }

}
