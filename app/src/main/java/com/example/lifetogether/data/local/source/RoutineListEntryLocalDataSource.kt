package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.RoutineListsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineListEntryLocalDataSource @Inject constructor(
    private val routineListsDao: RoutineListsDao,
) {
    suspend fun updateRoutineListEntries(
        items: List<RoutineListEntry>,
        byteArrays: Map<String, ByteArray> = emptyMap(),
    ) {
        if (items.isEmpty()) return
        val currentItems = routineListsDao.getItems(items.first().familyId).first()
        var entities = items.map { it.toEntity() }
        entities = entities.map { entity ->
            val existingImage = currentItems.find { it.id == entity.id }?.imageData
            val newImage = byteArrays[entity.id]
            entity.copy(imageData = newImage ?: existingImage)
        }
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
        routineListsDao.updateItems(itemsToUpdate)
        if (itemsToDelete.isNotEmpty()) {
            routineListsDao.deleteItems(itemsToDelete.map { it.id })
        }
    }

    suspend fun getRoutineEntryIdsWithImages(familyId: String): List<String> =
        routineListsDao.getEntryIdsWithImages(familyId)

    fun deleteFamilyRoutineListEntries(familyId: String) {
        routineListsDao.deleteFamilyItems(familyId)
    }

    fun deleteItems(itemIds: List<String>): ResultListener =
        try {
            routineListsDao.deleteItems(itemIds)
            ResultListener.Success
        } catch (e: Exception) {
            ResultListener.Failure("Error: ${e.message}")
        }

    fun getImageByteArray(entryId: String) = routineListsDao.getImageByteArray(entryId)

    private fun RoutineListEntry.toEntity() = RoutineListEntryEntity(
        id = id ?: "",
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        nextDate = nextDate,
        lastCompletedAt = lastCompletedAt,
        completionCount = completionCount,
        recurrenceUnit = recurrenceUnit,
        interval = interval,
        weekdays = weekdays,
        imageData = null,
    )
}
