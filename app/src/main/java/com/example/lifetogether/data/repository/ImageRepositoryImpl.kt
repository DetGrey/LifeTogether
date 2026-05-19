package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOf

import com.example.lifetogether.domain.result.AppError

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FamilyFirestoreDataSource
import com.example.lifetogether.data.remote.RecipeFirestoreDataSource
import com.example.lifetogether.data.remote.UserFirestoreDataSource
import com.example.lifetogether.data.remote.UserListFirestoreDataSource
import com.example.lifetogether.domain.model.image.UploadedImage
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.di.AppScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepositoryImpl @Inject constructor(
    @param:AppScope private val appScope: CoroutineScope,
    private val userLocalDataSource: UserLocalDataSource,
    private val recipeLocalDataSource: RecipeLocalDataSource,
    private val userListLocalDataSource: UserListLocalDataSource,
    private val storageDataSource: StorageDataSource,
    private val userFirestoreDataSource: UserFirestoreDataSource,
    private val familyFirestoreDataSource: FamilyFirestoreDataSource,
    private val recipeFirestoreDataSource: RecipeFirestoreDataSource,
    private val userListFirestoreDataSource: UserListFirestoreDataSource,
) : ImageRepository {
    companion object {
        private const val TAG = "LocalImageRepositoryImpl"
    }

    private val imageByteArrayFlows = ConcurrentHashMap<ImageType, Flow<Result<ByteArray, AppError>>>()

    override fun observeImageByteArray(imageType: ImageType): Flow<Result<ByteArray, AppError>> {
        return imageByteArrayFlows.getOrPut(imageType) {
            Log.d(TAG, "getImageByteArray")
            val byteArrayFlow = when (imageType) {
                is ImageType.ProfileImage -> userLocalDataSource.observeProfileImageByteArray(imageType.uid)
                is ImageType.FamilyImage -> userLocalDataSource.observeFamilyImageByteArray(imageType.familyId)
                is ImageType.RecipeImage -> recipeLocalDataSource.observeImageByteArray(
                    familyId = imageType.familyId,
                    recipeId = imageType.recipeId,
                )
                is ImageType.RoutineListEntryImage -> userListLocalDataSource.observeRoutineImageByteArray(imageType.entryId)
                is ImageType.GalleryMedia -> flowOf(null)
            }
            byteArrayFlow.map { byteArray ->
                appResultOf {
                    byteArray ?: throw AppErrorThrowable(AppErrors.storage("No ByteArray found"))
                }
            }.shareIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                replay = 1,
            )
        }
    }

    // ------------------- REMOTE
    override suspend fun uploadImage(
        uri: Uri,
        imageType: ImageType,
        context: Context,
    ): Result<UploadedImage, AppError> {
        return when (val uploadResult = storageDataSource.uploadPhoto(uri, imageType, context)) {
            is Result.Success -> {
                when (val persistResult = persistUploadedImageBytes(imageType, uploadResult.data.byteArray)) {
                    is Result.Success -> Result.Success(uploadResult.data)
                    is Result.Failure -> Result.Failure(persistResult.error)
                }
            }

            is Result.Failure -> Result.Failure(uploadResult.error)
        }
    }

    override suspend fun deleteImage(
        imageType: ImageType,
    ): Result<Unit, AppError> {
        val urlResult = when (imageType) {
            is ImageType.ProfileImage -> userFirestoreDataSource.getUserImageUrl(imageType.uid)
            is ImageType.FamilyImage -> familyFirestoreDataSource.getFamilyImageUrl(imageType.familyId)
            is ImageType.RecipeImage -> recipeFirestoreDataSource.getRecipeImageUrl(imageType.recipeId)
            is ImageType.RoutineListEntryImage -> userListFirestoreDataSource.getRoutineListEntryImageUrl(imageType.entryId)
            is ImageType.GalleryMedia -> Result.Failure(AppErrors.validation("Image type GalleryImage is not connected to one specific document"))
        }
        return when (urlResult) {
            is Result.Success -> {
                storageDataSource.deleteImage(urlResult.data)
            }
            is Result.Failure -> {
                if (urlResult.error is AppError.NotFound) Result.Success(Unit) else Result.Failure(urlResult.error)
            }
        }
    }

    override suspend fun deleteMediaFiles(
        urlList: List<String>,
    ): Result<Unit, AppError> {
        return storageDataSource.deleteImages(urlList)
    }

    override suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): Result<Unit, AppError> {
        val remoteResult = when (imageType) {
            is ImageType.ProfileImage -> userFirestoreDataSource.saveUserImageUrl(imageType.uid, url)
            is ImageType.FamilyImage -> familyFirestoreDataSource.saveFamilyImageUrl(imageType.familyId, url)
            is ImageType.RecipeImage -> recipeFirestoreDataSource.saveRecipeImageUrl(imageType.recipeId, url)
            is ImageType.RoutineListEntryImage -> userListFirestoreDataSource.saveRoutineListEntryImageUrl(imageType.entryId, url)
            is ImageType.GalleryMedia -> Result.Failure(AppErrors.validation("Image type is not connected to one specific document"))
        }
        return when (remoteResult) {
            is Result.Success -> {
                when (val localResult = persistImageUrl(imageType, url)) {
                    is Result.Success -> Result.Success(Unit)
                    is Result.Failure -> localResult
                }
            }

            is Result.Failure -> remoteResult
        }
    }

    private suspend fun persistUploadedImageBytes(
        imageType: ImageType,
        byteArray: ByteArray,
    ): Result<Unit, AppError> {
        return try {
            when (imageType) {
                is ImageType.ProfileImage -> userLocalDataSource.updateProfileImageByteArray(imageType.uid, byteArray)
                is ImageType.FamilyImage -> userLocalDataSource.updateFamilyImageByteArray(imageType.familyId, byteArray)
                is ImageType.RecipeImage -> recipeLocalDataSource.updateRecipeImageByteArray(
                    familyId = imageType.familyId,
                    recipeId = imageType.recipeId,
                    imageData = byteArray,
                )
                is ImageType.RoutineListEntryImage -> userListLocalDataSource.updateRoutineImageByteArray(
                    familyId = imageType.familyId,
                    entryId = imageType.entryId,
                    imageData = byteArray,
                )
                is ImageType.GalleryMedia -> Unit
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    private suspend fun persistImageUrl(
        imageType: ImageType,
        url: String,
    ): Result<Unit, AppError> {
        return try {
            when (imageType) {
                is ImageType.ProfileImage -> userLocalDataSource.updateProfileImageUrl(imageType.uid, url)
                is ImageType.FamilyImage -> userLocalDataSource.updateFamilyImageUrl(imageType.familyId, url)
                is ImageType.RecipeImage -> recipeLocalDataSource.updateRecipeImageUrl(
                    familyId = imageType.familyId,
                    recipeId = imageType.recipeId,
                    imageUrl = url,
                )
                is ImageType.RoutineListEntryImage -> userListLocalDataSource.updateRoutineImageUrl(
                    familyId = imageType.familyId,
                    entryId = imageType.entryId,
                    imageUrl = url,
                )
                is ImageType.GalleryMedia -> Unit
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }
}
