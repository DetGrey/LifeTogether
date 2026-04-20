package com.example.lifetogether.domain.usecase.image

import android.util.Log
import com.example.lifetogether.data.local.source.MediaLocalDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.SaveProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DownloadMediaUseCase @Inject constructor(
    private val mediaLocalDataSource: MediaLocalDataSource,
) {
    operator fun invoke(
        mediaIds: List<String>,
        familyId: String,
    ): Flow<SaveProgress> = flow {
        Log.d("DownloadMediaUseCase", "Attempting to download media")

        if (mediaIds.isEmpty()) {
            emit(SaveProgress.Finished(0, 0))
            return@flow
        }

        try {
            val items = mediaLocalDataSource.getMediaFilesForDownload(mediaIds, familyId)

            if (items.isNullOrEmpty()) {
                emit(SaveProgress.Error("No media items found"))
                return@flow
            }

            var successCount = 0
            var failureCount = 0

            items.forEachIndexed { index, (file, mediaItem) ->
                emit(SaveProgress.Loading(current = index + 1, total = items.size))

                if (mediaItem == null) {
                    failureCount++
                    return@forEachIndexed
                }

                val result = mediaLocalDataSource.copyMediaToGalleryFolder(file, mediaItem)

                if (result is Result.Success) {
                    successCount++
                } else {
                    failureCount++
                }
            }

            emit(SaveProgress.Finished(successCount, failureCount))

        } catch (e: Exception) {
            Log.e("DownloadMediaUseCase", "Critical failure", e)
            emit(SaveProgress.Error("Unexpected error: ${e.message}"))
        }
    }
}
