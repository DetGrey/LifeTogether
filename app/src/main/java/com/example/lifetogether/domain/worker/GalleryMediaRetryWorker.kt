package com.example.lifetogether.domain.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.example.lifetogether.domain.repository.GalleryRepository

class GalleryMediaRetryWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GalleryMediaRetryWorkerEntryPoint {
        fun galleryRepository(): GalleryRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            GalleryMediaRetryWorkerEntryPoint::class.java,
        )
        val familyId = inputData.getString(KEY_FAMILY_ID).orEmpty()
        val mediaIds = inputData.getStringArray(KEY_MEDIA_IDS)?.toList().orEmpty()
        if (familyId.isBlank() || mediaIds.isEmpty()) {
            return Result.failure()
        }

        return when (entryPoint.galleryRepository().retryGalleryMediaDownloads(mediaIds, familyId)) {
            is com.example.lifetogether.domain.result.Result.Success -> Result.success()
            is com.example.lifetogether.domain.result.Result.Failure -> Result.retry()
        }
    }

    companion object {
        const val WORK_NAME_PREFIX = "gallery-media-retry"
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_MEDIA_IDS = "media_ids"

        fun createInputData(
            familyId: String,
            mediaIds: List<String>,
        ): Data = Data.Builder()
            .putString(KEY_FAMILY_ID, familyId)
            .putStringArray(KEY_MEDIA_IDS, mediaIds.toTypedArray())
            .build()
    }
}
