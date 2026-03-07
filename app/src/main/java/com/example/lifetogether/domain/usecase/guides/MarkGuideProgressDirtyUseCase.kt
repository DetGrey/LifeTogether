package com.example.lifetogether.domain.usecase.guides

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideProgressState
import java.util.Date
import javax.inject.Inject

class MarkGuideProgressDirtyUseCase @Inject constructor(
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        guide: Guide,
        uid: String,
    ): Boolean {
        val progress = GuideProgressState.fromGuide(
            guide = guide,
            uid = uid,
            pendingSync = true,
            localUpdatedAt = Date(),
        ) ?: return false
        localDataSource.upsertGuideProgress(progress)
        return true
    }
}
