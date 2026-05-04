package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface UserListRepository {
    fun observeUserLists(familyId: String): Flow<Result<List<UserList>, AppError>>
    fun syncUserListsFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    suspend fun saveUserList(userList: UserList): Result<String, AppError>
    fun observeRoutineListEntriesByListId(familyId: String, listId: String): Flow<Result<List<RoutineListEntry>, AppError>>
    fun syncRoutineListEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    fun observeRoutineListEntry(id: String): Flow<Result<RoutineListEntry, AppError>>
    fun observeRoutineImageByteArray(entryId: String): Flow<Result<ByteArray, AppError>>
    suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, AppError>
    suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, AppError>
    suspend fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, AppError>

    fun observeWishListEntriesByListId(familyId: String, listId: String): Flow<Result<List<WishListEntry>, AppError>>
    fun syncWishListEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    fun observeWishListEntry(id: String): Flow<Result<WishListEntry, AppError>>
    suspend fun saveWishListEntry(entry: WishListEntry): Result<String, AppError>
    suspend fun updateWishListEntry(entry: WishListEntry): Result<Unit, AppError>
    suspend fun deleteWishListEntries(itemIds: List<String>): Result<Unit, AppError>

    fun observeNoteEntriesByListId(familyId: String, listId: String): Flow<Result<List<NoteEntry>, AppError>>
    fun syncNoteEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    fun observeNoteEntry(id: String): Flow<Result<NoteEntry, AppError>>
    suspend fun saveNoteEntry(entry: NoteEntry): Result<String, AppError>
    suspend fun updateNoteEntry(entry: NoteEntry): Result<Unit, AppError>
    suspend fun deleteNoteEntries(itemIds: List<String>): Result<Unit, AppError>

    fun observeChecklistEntriesByListId(familyId: String, listId: String): Flow<Result<List<ChecklistEntry>, AppError>>
    fun syncChecklistEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    fun observeChecklistEntry(id: String): Flow<Result<ChecklistEntry, AppError>>
    suspend fun saveChecklistEntry(entry: ChecklistEntry): Result<String, AppError>
    suspend fun updateChecklistEntry(entry: ChecklistEntry): Result<Unit, AppError>
    suspend fun deleteChecklistEntries(itemIds: List<String>): Result<Unit, AppError>

    fun observeMealPlanEntriesByListId(familyId: String, listId: String): Flow<Result<List<MealPlanEntry>, AppError>>
    fun syncMealPlanEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    fun observeMealPlanEntry(id: String): Flow<Result<MealPlanEntry, AppError>>
    suspend fun saveMealPlanEntry(entry: MealPlanEntry): Result<String, AppError>
    suspend fun updateMealPlanEntry(entry: MealPlanEntry): Result<Unit, AppError>
    suspend fun deleteMealPlanEntries(itemIds: List<String>): Result<Unit, AppError>
}
