package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.RoutineListsDao
import com.example.lifetogether.data.local.dao.UserListsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.domain.model.lists.UserList
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserListLocalDataSource @Inject constructor(
    private val userListsDao: UserListsDao,
    private val routineListsDao: RoutineListsDao,
) {
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
