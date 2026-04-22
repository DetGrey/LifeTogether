package com.example.lifetogether.domain.usecase.sync

import android.content.Context
import android.util.Log
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SyncGalleryMediaUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
) {
    companion object {
        private const val TAG = "ObserveGalleryMedia"
    }

    fun start(
        scope: CoroutineScope,
        familyId: String,
        context: Context,
    ): SyncStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            Log.d(TAG, "invoked")
            galleryRepository.syncGalleryMediaFromRemote(familyId, context).collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> Log.e(TAG, "failure: ${result.error}")
                }
            }
        }
        return SyncStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
