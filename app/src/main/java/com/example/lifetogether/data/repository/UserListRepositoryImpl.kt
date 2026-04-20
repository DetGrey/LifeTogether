package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.local.source.query.ListQueryType
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserListRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val userListLocalDataSource: UserListLocalDataSource,
): UserListRepository {
    companion object {
        val userListsType = ListQueryType.UserLists //todo rename to something better
    }

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

    override suspend fun saveUserList(userList: UserList): Result<String, String> {
        return firestoreDataSource.saveItem(userList, userListsType.tableName)
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
        return firestoreDataSource.saveItem(entry, Constants.ROUTINE_LIST_ENTRIES_TABLE)
    }

    override suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, String> {
        return firestoreDataSource.updateItem(entry, Constants.ROUTINE_LIST_ENTRIES_TABLE)
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