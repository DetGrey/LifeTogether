package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.logic.appResultOf

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.dao.RoutineListsDao
import com.example.lifetogether.data.local.dao.UserListsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserListLocalDataSource @Inject constructor(
    private val userListsDao: UserListsDao,
    private val routineListsDao: RoutineListsDao,
) {
    fun observeUserLists(familyId: String): Flow<List<UserListEntity>> {
        return userListsDao.getItems(familyId)
    }

    suspend fun upsertUserLists(items: List<UserList>) {
        if (items.isEmpty()) return
        val entities = items.map { it.toEntity() }
        val currentItems = userListsDao.getItems(items.first().familyId).first()
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        userListsDao.updateItems(itemsToUpdate)
    }

    suspend fun updateUserLists(items: List<UserList>) {
        if (items.isEmpty()) return
        val entities = items.map { it.toEntity() }
        val currentItems = userListsDao.getItems(items.first().familyId).first()
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
        userListsDao.updateItems(itemsToUpdate)
        if (itemsToDelete.isNotEmpty()) {
            val deletedIds = itemsToDelete.map { it.id }
            routineListsDao.deleteByListIds(deletedIds)
            userListsDao.deleteItems(deletedIds)
        }
    }

    fun deleteFamilyUserLists(familyId: String) {
        routineListsDao.deleteFamilyItems(familyId)
        userListsDao.deleteFamilyItems(familyId)
    }

    fun observeRoutineListEntries(familyId: String): Flow<List<RoutineListEntryEntity>> {
        return routineListsDao.getItems(familyId)
    }

    fun observeRoutineListEntry(id: String): Flow<RoutineListEntryEntity> {
        return routineListsDao.getItemById(id)
    }

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

    fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, AppError> =
        appResultOf {
            routineListsDao.deleteItems(itemIds)
        }

    fun observeRoutineImageByteArray(entryId: String) = routineListsDao.getImageByteArray(entryId)

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

    private fun UserList.toEntity() = UserListEntity(
        id = id ?: "",
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        type = type,
        visibility = visibility,
        ownerUid = ownerUid,
    )
}
