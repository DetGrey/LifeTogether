package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.TipTrackerDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.domain.model.TipItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipTrackerLocalDataSource @Inject constructor(
    private val tipTrackerDao: TipTrackerDao,
) {
    suspend fun updateTipTracker(items: List<TipItem>) {
        val familyId = items.firstOrNull()?.familyId ?: return
        val entities = items.map { item ->
            TipEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                amount = item.amount,
                currency = item.currency,
                date = item.date,
            )
        }
        val currentItems = tipTrackerDao.getItems(familyId).first()
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
        tipTrackerDao.updateItems(itemsToUpdate)
        tipTrackerDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyTipItems(familyId: String) {
        tipTrackerDao.getItems(familyId).firstOrNull()?.let { currentFamilyItems ->
            tipTrackerDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }
}
