package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface UserListRepository {
    fun observeUserLists(familyId: String): Flow<Result<List<UserList>, AppError>>
    fun syncUserListsFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    suspend fun saveUserList(userList: UserList): Result<String, AppError>
    fun observeRoutineListEntries(familyId: String): Flow<Result<List<RoutineListEntry>, AppError>>
    fun syncRoutineListEntriesFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    fun observeRoutineListEntry(id: String): Flow<Result<RoutineListEntry, AppError>>
    fun observeRoutineImageByteArray(entryId: String): Flow<Result<ByteArray, AppError>>
    suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, AppError>
    suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, AppError>
    suspend fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, AppError>
}
