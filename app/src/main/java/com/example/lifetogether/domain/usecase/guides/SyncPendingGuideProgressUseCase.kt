package com.example.lifetogether.domain.usecase.guides

import android.util.Log
import com.example.lifetogether.data.local.source.GuideProgressLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ResultListener
import java.util.Date
import javax.inject.Inject

class SyncPendingGuideProgressUseCase @Inject constructor(
    private val guideProgressLocalDataSource: GuideProgressLocalDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) {
    private companion object {
        const val TAG = "SyncGuideProgressUseCase"
        const val MIN_UPLOAD_INTERVAL_MS = 2 * 60 * 1000L
    }

    suspend operator fun invoke(
        familyId: String,
        uid: String,
        force: Boolean = false,
        guideId: String? = null,
    ) {
        val pendingItems = guideProgressLocalDataSource.getPendingGuideProgresses(
            familyId = familyId,
            uid = uid,
            guideId = guideId,
        )
        if (pendingItems.isEmpty()) return

        val now = Date()
        pendingItems.forEach { progress ->
            val lastCheckpointTime = progress.lastUploadedAt?.time ?: progress.localUpdatedAt.time
            if (!force && now.time - lastCheckpointTime < MIN_UPLOAD_INTERVAL_MS) {
                return@forEach
            }

            val uploadCandidate = progress.copy(lastUploadedAt = now)
            when (val result = firestoreDataSource.updateGuideProgress(uploadCandidate)) {
                is ResultListener.Success -> {
                    guideProgressLocalDataSource.markGuideProgressSynced(progress.id, now)
                }

                is ResultListener.Failure -> {
                    Log.e(TAG, "Failed syncing guide progress id=${progress.id}: ${result.message}")
                }
            }
        }
    }
}
