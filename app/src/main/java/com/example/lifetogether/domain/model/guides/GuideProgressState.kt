package com.example.lifetogether.domain.model.guides

import com.example.lifetogether.domain.logic.GuideProgressSnapshot
import java.util.Date

data class GuideProgressState(
    val id: String,
    val familyId: String,
    val uid: String,
    val guideId: String,
    val contentVersion: Long,
    val started: Boolean,
    val completedPointerKeys: List<String>,
    val resume: GuideResume?,
    val lastUpdated: Date,
    val pendingSync: Boolean = true,
    val localUpdatedAt: Date = Date(),
    val lastUploadedAt: Date? = null,
) {
    companion object {
        fun documentId(
            familyId: String,
            uid: String,
            guideId: String,
        ): String = "${familyId}_${uid}_$guideId"

        fun fromGuide(
            guide: Guide,
            uid: String,
            pendingSync: Boolean = true,
            localUpdatedAt: Date = Date(),
            lastUploadedAt: Date? = null,
        ): GuideProgressState? {
            val resolvedGuideId = guide.id?.takeIf { it.isNotBlank() } ?: return null
            return GuideProgressState(
                id = documentId(guide.familyId, uid, resolvedGuideId),
                familyId = guide.familyId,
                uid = uid,
                guideId = resolvedGuideId,
                contentVersion = guide.contentVersion,
                started = guide.started,
                completedPointerKeys = GuideProgressSnapshot
                    .completedPointerKeysFromSections(guide.sections)
                    .sorted(),
                resume = guide.resume,
                lastUpdated = guide.lastUpdated,
                pendingSync = pendingSync,
                localUpdatedAt = localUpdatedAt,
                lastUploadedAt = lastUploadedAt,
            )
        }
    }
}
