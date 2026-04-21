package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.local.source.query.ListQueryType
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.data.remote.UserListFirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserListRepositoryImpl @Inject constructor(
    private val userListFirestoreDataSource: UserListFirestoreDataSource,
    private val userListLocalDataSource: UserListLocalDataSource,
    private val storageDataSource: StorageDataSource,
): UserListRepository {
    // USER LIST
    override fun observeUserLists(familyId: String): Flow<Result<List<UserList>, String>> {
        return userListLocalDataSource.observeUserLists(familyId)
            .map { entities ->
                try {
                    val userLists = entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                    Result.Success(userLists)
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override fun syncUserListsFromRemote(uid: String, familyId: String): Flow<Result<Unit, String>> {
        return combine(
            userListFirestoreDataSource.familySharedUserListsSnapshotListener(familyId),
            userListFirestoreDataSource.privateUserListsSnapshotListener(familyId, uid),
        ) { sharedResult, privateResult ->
            val shared = when (sharedResult) {
                is Result.Success -> sharedResult.data.items
                is Result.Failure -> emptyList()
            }
            val private = when (privateResult) {
                is Result.Success -> privateResult.data.items
                is Result.Failure -> emptyList()
            }

            val hadSuccess = sharedResult is Result.Success || privateResult is Result.Success
            if (!hadSuccess) {
                return@combine Result.Failure(
                    listOfNotNull(
                        (sharedResult as? Result.Failure)?.error,
                        (privateResult as? Result.Failure)?.error,
                    ).joinToString(" | ").ifBlank { "User list sync failed" },
                )
            }

            runCatching {
                val merged = (shared + private)
                    .associateBy { it.id ?: "" }
                    .values
                    .filter { !it.id.isNullOrBlank() }

                if (merged.isEmpty()) {
                    userListLocalDataSource.deleteFamilyUserLists(familyId)
                } else {
                    userListLocalDataSource.updateUserLists(merged.toList())
                }
                Result.Success(Unit)
            }.getOrElse { error ->
                Result.Failure(error.message ?: "Failed to sync user lists")
            }
        }
    }

    override suspend fun saveUserList(userList: UserList): Result<String, String> {
        return userListFirestoreDataSource.saveUserList(userList)
    }
    
    // ROUTINE ENTRY
    override fun observeRoutineListEntries(familyId: String): Flow<Result<List<RoutineListEntry>, String>> {
        return userListLocalDataSource.observeRoutineListEntries(familyId)
            .map { entities ->
                try {
                    val routineListEntry = entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                    Result.Success(routineListEntry)
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override fun syncRoutineListEntriesFromRemote(familyId: String): Flow<Result<Unit, String>> {
        return userListFirestoreDataSource.familyRoutineListEntriesSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> runCatching {
                    if (result.data.items.isEmpty()) {
                        userListLocalDataSource.deleteFamilyRoutineListEntries(familyId)
                    } else {
                        val existingIdsWithImages = userListLocalDataSource.getRoutineEntryIdsWithImages(familyId)
                        val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                        for (entry in result.data.items) {
                            if (entry.id != null && existingIdsWithImages.contains(entry.id)) continue
                            val byteArrayResult = entry.imageUrl?.let { url ->
                                storageDataSource.fetchImageByteArray(url)
                            }
                            if (byteArrayResult is Result.Success) {
                                entry.id?.let { byteArrays[it] = byteArrayResult.data }
                            }
                        }
                        userListLocalDataSource.updateRoutineListEntries(result.data.items, byteArrays)
                    }
                    Result.Success(Unit)
                }.getOrElse { error ->
                    Result.Failure(error.message ?: "Failed to sync routine list entries")
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override fun observeRoutineListEntry(id: String): Flow<Result<RoutineListEntry, String>> {
        return userListLocalDataSource.observeRoutineListEntry(id)
            .map { entity ->
                try {
                    val routineListEntry = entity.toModel()
                    Result.Success(routineListEntry)
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override fun observeRoutineImageByteArray(entryId: String): Flow<Result<ByteArray, String>> {
        val byteArrayFlow = userListLocalDataSource.observeRoutineImageByteArray(entryId)
        return byteArrayFlow.map { byteArray ->
            try {
                if (byteArray != null) {
                    Result.Success(byteArray)
                } else {
                    Result.Failure("No ByteArray found")
                }
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown error")
            }
        }
    }

    override suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, String> {
        return userListFirestoreDataSource.saveRoutineListEntry(entry)
    }

    override suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, String> {
        return userListFirestoreDataSource.updateRoutineListEntry(entry)
    }

    override suspend fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, String> {
        val remoteDelete = userListFirestoreDataSource.deleteRoutineListEntries(itemIds)
        if (remoteDelete is Result.Failure) return remoteDelete
        return userListLocalDataSource.deleteRoutineListEntries(itemIds)
    }
    
    private fun UserListEntity.toModel() = UserList(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        type = type,
        visibility = visibility,
        ownerUid = ownerUid,
    )
    private fun RoutineListEntryEntity.toModel() = RoutineListEntry(
        id = id,
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
    )
}
