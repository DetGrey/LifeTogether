package com.example.lifetogether.data.local.source

import android.util.Log
import com.example.lifetogether.data.local.dao.GuideProgressDao
import com.example.lifetogether.data.local.dao.GuidesDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.data.model.GuideEntity
import com.example.lifetogether.data.model.GuideProgressEntity
import com.example.lifetogether.domain.logic.GuideProgressSnapshot
import com.example.lifetogether.domain.model.guides.Guide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GuideLocalDataSource @Inject constructor(
    private val guidesDao: GuidesDao,
    private val guideProgressDao: GuideProgressDao,
) {
    companion object {
        private const val TAG = "GuideLocalDataSource"
    }

    fun getItems(
        familyId: String,
        uid: String? = null,
    ): Flow<List<Entity.Guide>> {
        return if (uid.isNullOrBlank()) {
            guidesDao.getItems(familyId).map { list ->
                list.map { Entity.Guide(it) }
            }
        } else {
            combine(
                guidesDao.getItems(familyId),
                guideProgressDao.getItems(familyId, uid),
            ) { guides, progressItems ->
                val progressByGuideId = progressItems.associateBy { it.guideId }
                guides.map { guide ->
                    Entity.Guide(
                        applyProgressToGuideEntity(
                            guide = guide,
                            progress = progressByGuideId[guide.id],
                        ),
                    )
                }
            }
        }
    }

    fun getItemById(
        familyId: String,
        id: String,
        uid: String? = null,
    ): Flow<Entity.Guide> {
        return flow {
            val guideFlow = guidesDao.getItemByIdFlow(familyId, id)
            val mergedFlow = if (uid.isNullOrBlank()) {
                guideFlow.map { guide -> guide?.let { Entity.Guide(it) } }
            } else {
                combine(
                    guideFlow,
                    guideProgressDao.getItemByGuideIdFlow(familyId, uid, id),
                ) { guide, progress ->
                    guide?.let { Entity.Guide(applyProgressToGuideEntity(it, progress)) }
                }
            }
            emitAll(mergedFlow.mapNotNull { it })
        }
    }

    suspend fun upsertGuides(items: List<Guide>) {
        if (items.isEmpty()) return
        val entities = items.map { item ->
            GuideEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                description = item.description,
                visibility = item.visibility,
                ownerUid = item.ownerUid,
                contentVersion = item.contentVersion,
                started = item.started,
                sections = item.sections,
                resume = item.resume,
            )
        }
        val currentItems = guidesDao.getItems(items.first().familyId).first()
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        guidesDao.updateItems(itemsToUpdate)
    }

    suspend fun updateGuides(items: List<Guide>) {
        if (items.isEmpty()) return
        val entities = items.map { item ->
            GuideEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                description = item.description,
                visibility = item.visibility,
                ownerUid = item.ownerUid,
                contentVersion = item.contentVersion,
                started = item.started,
                sections = item.sections,
                resume = item.resume,
            )
        }
        val currentItems = guidesDao.getItems(items.first().familyId).first()
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        val itemsToDelete = computeItemsToDelete(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        val deletedGuideIds = itemsToDelete.map { it.id }

        guidesDao.updateItems(itemsToUpdate)
        if (deletedGuideIds.isNotEmpty()) {
            guideProgressDao.deleteByGuideIds(items.first().familyId, deletedGuideIds)
            guidesDao.deleteItems(deletedGuideIds)
        }
    }

    suspend fun deleteFamilyGuides(familyId: String) {
        val currentFamilyItems = guidesDao.getItems(familyId).first()
        if (currentFamilyItems.isNotEmpty()) {
            Log.d(TAG, "deleteFamilyGuides familyId=$familyId count=${currentFamilyItems.size}")
            val deletedGuideIds = currentFamilyItems.map { it.id }
            guideProgressDao.deleteByGuideIds(familyId, deletedGuideIds)
            guidesDao.deleteItems(deletedGuideIds)
        }
    }

    private fun applyProgressToGuideEntity(
        guide: GuideEntity,
        progress: GuideProgressEntity?,
    ): GuideEntity {
        if (progress == null) return guide
        if (progress.contentVersion != guide.contentVersion) {
            return guide.copy(
                started = false,
                resume = null,
                sections = GuideProgressSnapshot.applyCompletedPointerKeys(
                    sections = guide.sections,
                    completedPointerKeys = emptySet(),
                ),
            )
        }
        val mergedLastUpdated = if (progress.lastUpdated.after(guide.lastUpdated)) progress.lastUpdated else guide.lastUpdated
        return guide.copy(
            started = progress.started,
            sections = GuideProgressSnapshot.applyCompletedPointerKeys(
                sections = guide.sections,
                completedPointerKeys = progress.completedPointerKeys.toSet(),
            ),
            resume = progress.resume,
            lastUpdated = mergedLastUpdated,
        )
    }
}
