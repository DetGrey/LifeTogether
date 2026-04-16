package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface UserListRepository {
    fun observeUserLists(familyId: String): Flow<Result<List<UserList>, String>>
    suspend fun saveUserList(userList: UserList): Result<String, String>
    fun observeRoutineListEntries(familyId: String): Flow<Result<List<RoutineListEntry>, String>>
    fun observeRoutineListEntry(id: String): Flow<Result<RoutineListEntry, String>>
    fun observeRoutineImageByteArray(entryId: String): Flow<Result<ByteArray, String>>
    suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, String>
    suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, String>
}
