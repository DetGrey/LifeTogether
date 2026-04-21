package com.example.lifetogether.domain.usecase.observers

import android.util.Log
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveAlbumsUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
) {
    private companion object {
        const val TAG = "ObserveAlbumsUC"
    }

    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            galleryRepository.syncAlbumsFromRemote(familyId).collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> Log.e(TAG, "albums sync failure: ${result.error}")
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
