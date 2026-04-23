package com.example.lifetogether.domain.usecase.sync

import android.util.Log
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.result.Result as AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class SyncGuidesUseCase @Inject constructor(
    private val guideRepository: GuideRepository,
) {
    private companion object {
        const val TAG = "SyncGuidesUseCase"
    }

    fun start(
        scope: CoroutineScope,
        uid: String,
        familyId: String,
    ): SyncStartHandle {
        val firstSuccess = CompletableDeferred<kotlin.Result<Unit>>()
        val job = scope.launch {
            Log.d(TAG, "invoke uid=$uid familyId=$familyId")
            guideRepository.syncGuidesFromRemote(uid, familyId).collect { result ->
                when (result) {
                    is AppResult.Success -> firstSuccess.completeFirstSuccessIfNeeded()
                    is AppResult.Failure -> Log.e(TAG, "guides sync failure: ${result.error}")
                }
            }
        }
        return SyncStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
