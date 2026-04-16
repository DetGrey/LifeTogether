package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.GuideProgressDao
import com.example.lifetogether.data.model.GuideProgressEntity
import com.example.lifetogether.domain.model.guides.GuideProgressState
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuideProgressLocalDataSource @Inject constructor(
    private val guideProgressDao: GuideProgressDao,
) {
    suspend fun upsertGuideProgress(progress: GuideProgressState) {
        val existing = guideProgressDao.getItemById(progress.id)
        val entity = GuideProgressEntity(
            id = progress.id,
            familyId = progress.familyId,
            uid = progress.uid,
            guideId = progress.guideId,
            contentVersion = progress.contentVersion,
            started = progress.started,
            completedPointerKeys = progress.completedPointerKeys,
            resume = progress.resume,
            lastUpdated = progress.lastUpdated,
            pendingSync = true,
            localUpdatedAt = progress.localUpdatedAt,
            lastUploadedAt = existing?.lastUploadedAt,
        )
        guideProgressDao.upsertItem(entity)
    }

    suspend fun updateGuideProgressFromRemote(
        familyId: String,
        uid: String,
        items: List<GuideProgressState>,
    ) {
        val entityList = mutableListOf<GuideProgressEntity>()
        val protectedGuideIds = mutableSetOf<String>()
        items.forEach { item ->
            val existing = guideProgressDao.getItemById(item.id)
            if (existing?.pendingSync == true) {
                protectedGuideIds += existing.guideId
                return@forEach
            }
            entityList += GuideProgressEntity(
                id = item.id,
                familyId = item.familyId,
                uid = item.uid,
                guideId = item.guideId,
                contentVersion = item.contentVersion,
                started = item.started,
                completedPointerKeys = item.completedPointerKeys,
                resume = item.resume,
                lastUpdated = item.lastUpdated,
                pendingSync = false,
                localUpdatedAt = item.localUpdatedAt,
                lastUploadedAt = item.lastUploadedAt ?: item.lastUpdated,
            )
        }

        if (entityList.isNotEmpty()) {
            guideProgressDao.upsertItems(entityList)
        }

        val localPendingGuideIds = guideProgressDao.getPendingItems(familyId, uid)
            .map { it.guideId }
            .toSet()
        val keepGuideIds = items.map { it.guideId }.toSet() + localPendingGuideIds + protectedGuideIds

        if (keepGuideIds.isEmpty()) {
            guideProgressDao.deleteFamilyUserItems(familyId, uid)
            return
        }
        guideProgressDao.deleteMissingGuides(familyId, uid, keepGuideIds.toList())
    }

    suspend fun getPendingGuideProgresses(
        familyId: String,
        uid: String,
        guideId: String? = null,
    ): List<GuideProgressState> {
        val allPending = guideProgressDao.getPendingItems(familyId, uid)
        val filtered = if (guideId.isNullOrBlank()) {
            allPending
        } else {
            allPending.filter { it.guideId == guideId }
        }
        return filtered.map { entity ->
            GuideProgressState(
                id = entity.id,
                familyId = entity.familyId,
                uid = entity.uid,
                guideId = entity.guideId,
                contentVersion = entity.contentVersion,
                started = entity.started,
                completedPointerKeys = entity.completedPointerKeys,
                resume = entity.resume,
                lastUpdated = entity.lastUpdated,
                pendingSync = entity.pendingSync,
                localUpdatedAt = entity.localUpdatedAt,
                lastUploadedAt = entity.lastUploadedAt,
            )
        }
    }

    suspend fun markGuideProgressSynced(
        progressId: String,
        uploadedAt: Date,
    ) {
        val existing = guideProgressDao.getItemById(progressId) ?: return
        guideProgressDao.upsertItem(
            existing.copy(
                pendingSync = false,
                lastUploadedAt = uploadedAt,
            ),
        )
    }
}
