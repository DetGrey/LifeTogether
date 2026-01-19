package com.example.lifetogether.domain.usecase.image

import android.util.Log
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.callback.ResultListener
import javax.inject.Inject

class DownloadMediaUseCase @Inject constructor(
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        mediaId: String,
        familyId: String,
        fileName: String,
    ): ResultListener {
        return try {
            Log.d("DownloadMediaUseCase", "Attempting to download media: $mediaId")

            val mediaFileWithItem = localDataSource.getMediaFileForDownload(mediaId, familyId)
            if (mediaFileWithItem == null) {
                Log.w("DownloadMediaUseCase", "Media file not found: $mediaId")
                return ResultListener.Failure("Media file not found")
            }

            val (mediaFile, mediaItem) = mediaFileWithItem
            val result = localDataSource.copyMediaToGalleryFolder(mediaFile, mediaId, fileName, mediaItem)
            if (result is ResultListener.Success) {
                Log.d("DownloadMediaUseCase", "Media downloaded successfully: $mediaId")
            }
            result
        } catch (e: Exception) {
            Log.e("DownloadMediaUseCase", "Error downloading media: ${e.message}", e)
            ResultListener.Failure("Error downloading media: ${e.message}")
        }
    }
}
