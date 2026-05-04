package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.model.ChecklistEntryEntity
import com.example.lifetogether.data.model.MealPlanEntryEntity
import com.example.lifetogether.data.model.NoteEntryEntity
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.data.model.WishListEntryEntity
import com.example.lifetogether.data.remote.UserListFirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.ListEntry
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.ListSnapshot
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
    override fun observeUserLists(familyId: String): Flow<Result<List<UserList>, AppError>> {
        return userListLocalDataSource.observeUserLists(familyId)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedBy { it.itemName }
                }
            }
    }

    override fun syncUserListsFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>> {
        return combine(
            userListFirestoreDataSource.familySharedUserListsSnapshotListener(familyId),
            userListFirestoreDataSource.privateUserListsSnapshotListener(familyId, uid),
        ) { sharedResult, privateResult ->
            when (val accessibleListsResult = resolveAccessibleUserLists(
                sharedResult = sharedResult,
                privateResult = privateResult,
                failureMessage = "User list sync failed",
            )) {
                is Result.Failure -> accessibleListsResult
                is Result.Success -> appResultOfSuspend {
                    val merged = (accessibleListsResult.data.shared + accessibleListsResult.data.private)
                    .associateBy { it.id }
                    .values
                    .filter { it.id.isNotBlank() }

                    if (merged.isEmpty()) {
                        userListLocalDataSource.deleteFamilyUserLists(familyId)
                    } else {
                        userListLocalDataSource.updateUserLists(merged.toList())
                    }
                }
            }
        }
    }

    override suspend fun saveUserList(userList: UserList): Result<String, AppError> {
        return userListFirestoreDataSource.saveUserList(userList)
    }
    
    // ROUTINE ENTRY
    override fun observeRoutineListEntriesByListId(
        familyId: String,
        listId: String,
    ): Flow<Result<List<RoutineListEntry>, AppError>> {
        return userListLocalDataSource.observeRoutineListEntriesByListId(familyId, listId)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedBy { it.nextDate }
                }
            }
    }

    override fun syncRoutineListEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>> {
        return syncAccessibleEntriesFromRemote(
            uid = uid,
            familyId = familyId,
            listType = ListType.ROUTINE,
            entryFlow = userListFirestoreDataSource.familyRoutineListEntriesSnapshotListener(familyId),
            failureMessage = "Routine list sync failed",
            onEmptyEntries = {
                userListLocalDataSource.deleteFamilyRoutineListEntries(familyId)
            },
            onVisibleEntries = { visibleEntries ->
                val existingIdsWithImages = userListLocalDataSource.getRoutineEntryIdsWithImages(familyId)
                val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                for (entry in visibleEntries) {
                    if (existingIdsWithImages.contains(entry.id)) continue
                    val byteArrayResult = entry.imageUrl?.let { url ->
                        storageDataSource.fetchImageByteArray(url)
                    }
                    if (byteArrayResult is Result.Success) {
                        byteArrays[entry.id] = byteArrayResult.data
                    }
                }
                userListLocalDataSource.updateRoutineListEntries(
                    visibleEntries,
                    byteArrays,
                )
            },
        )
    }

    override fun observeRoutineListEntry(id: String): Flow<Result<RoutineListEntry, AppError>> {
        return userListLocalDataSource.observeRoutineListEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override fun observeRoutineImageByteArray(entryId: String): Flow<Result<ByteArray, AppError>> {
        val byteArrayFlow = userListLocalDataSource.observeRoutineImageByteArray(entryId)
        return byteArrayFlow.map { byteArray ->
            appResultOf {
                byteArray ?: throw AppErrorThrowable(AppErrors.storage("No ByteArray found"))
            }
        }
    }

    override suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, AppError> {
        return userListFirestoreDataSource.saveRoutineListEntry(entry)
    }

    override suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, AppError> {
        return userListFirestoreDataSource.updateRoutineListEntry(entry)
    }

    override suspend fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, AppError> {
        val remoteDelete = userListFirestoreDataSource.deleteRoutineListEntries(itemIds)
        if (remoteDelete is Result.Failure) return remoteDelete
        return userListLocalDataSource.deleteRoutineListEntries(itemIds)
    }

    // WISH LIST ENTRY
    override fun observeWishListEntriesByListId(
        familyId: String,
        listId: String,
    ): Flow<Result<List<WishListEntry>, AppError>> {
        return userListLocalDataSource.observeWishListEntriesByListId(familyId, listId)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedWith(
                            compareBy<WishListEntry> { it.isPurchased }
                                .thenBy { it.priority.ordinal }
                                .thenByDescending { it.lastUpdated },
                        )
                }
            }
    }

    override fun syncWishListEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>> {
        return syncAccessibleEntriesFromRemote(
            uid = uid,
            familyId = familyId,
            listType = ListType.WISH_LIST,
            entryFlow = userListFirestoreDataSource.familyWishListEntriesSnapshotListener(familyId),
            failureMessage = "Wish list sync failed",
            onEmptyEntries = {
                userListLocalDataSource.deleteFamilyWishListEntries(familyId)
            },
            onVisibleEntries = { visibleEntries ->
                userListLocalDataSource.updateWishListEntries(visibleEntries)
            },
        )
    }

    override fun observeWishListEntry(id: String): Flow<Result<WishListEntry, AppError>> {
        return userListLocalDataSource.observeWishListEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveWishListEntry(entry: WishListEntry): Result<String, AppError> {
        return userListFirestoreDataSource.saveWishListEntry(entry)
    }

    override suspend fun updateWishListEntry(entry: WishListEntry): Result<Unit, AppError> {
        return userListFirestoreDataSource.updateWishListEntry(entry)
    }

    override suspend fun deleteWishListEntries(itemIds: List<String>): Result<Unit, AppError> {
        val remoteDelete = userListFirestoreDataSource.deleteWishListEntries(itemIds)
        if (remoteDelete is Result.Failure) return remoteDelete
        return userListLocalDataSource.deleteWishListEntries(itemIds)
    }

    // NOTE ENTRY
    override fun observeNoteEntriesByListId(
        familyId: String,
        listId: String,
    ): Flow<Result<List<NoteEntry>, AppError>> {
        return userListLocalDataSource.observeNoteEntriesByListId(familyId, listId)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedByDescending { it.lastUpdated }
                }
            }
    }

    override fun syncNoteEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>> {
        return syncAccessibleEntriesFromRemote(
            uid = uid,
            familyId = familyId,
            listType = ListType.NOTES,
            entryFlow = userListFirestoreDataSource.familyNoteEntriesSnapshotListener(familyId),
            failureMessage = "Note entries sync failed",
            onEmptyEntries = {
                userListLocalDataSource.deleteFamilyNoteEntries(familyId)
            },
            onVisibleEntries = { visibleEntries ->
                userListLocalDataSource.updateNoteEntries(visibleEntries)
            },
        )
    }

    override fun observeNoteEntry(id: String): Flow<Result<NoteEntry, AppError>> {
        return userListLocalDataSource.observeNoteEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveNoteEntry(entry: NoteEntry): Result<String, AppError> {
        return userListFirestoreDataSource.saveNoteEntry(entry)
    }

    override suspend fun updateNoteEntry(entry: NoteEntry): Result<Unit, AppError> {
        return userListFirestoreDataSource.updateNoteEntry(entry)
    }

    override suspend fun deleteNoteEntries(itemIds: List<String>): Result<Unit, AppError> {
        val remoteDelete = userListFirestoreDataSource.deleteNoteEntries(itemIds)
        if (remoteDelete is Result.Failure) return remoteDelete
        return userListLocalDataSource.deleteNoteEntries(itemIds)
    }

    // CHECKLIST ENTRY
    override fun observeChecklistEntriesByListId(
        familyId: String,
        listId: String,
    ): Flow<Result<List<ChecklistEntry>, AppError>> {
        return userListLocalDataSource.observeChecklistEntriesByListId(familyId, listId)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedWith(
                            compareBy<ChecklistEntry> { it.isChecked }
                                .thenByDescending { it.lastUpdated },
                        )
                }
            }
    }

    override fun syncChecklistEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>> {
        return syncAccessibleEntriesFromRemote(
            uid = uid,
            familyId = familyId,
            listType = ListType.CHECKLIST,
            entryFlow = userListFirestoreDataSource.familyChecklistEntriesSnapshotListener(familyId),
            failureMessage = "Checklist entries sync failed",
            onEmptyEntries = {
                userListLocalDataSource.deleteFamilyChecklistEntries(familyId)
            },
            onVisibleEntries = { visibleEntries ->
                userListLocalDataSource.updateChecklistEntries(visibleEntries)
            },
        )
    }

    override fun observeChecklistEntry(id: String): Flow<Result<ChecklistEntry, AppError>> {
        return userListLocalDataSource.observeChecklistEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveChecklistEntry(entry: ChecklistEntry): Result<String, AppError> {
        return userListFirestoreDataSource.saveChecklistEntry(entry)
    }

    override suspend fun updateChecklistEntry(entry: ChecklistEntry): Result<Unit, AppError> {
        return userListFirestoreDataSource.updateChecklistEntry(entry)
    }

    override suspend fun deleteChecklistEntries(itemIds: List<String>): Result<Unit, AppError> {
        val remoteDelete = userListFirestoreDataSource.deleteChecklistEntries(itemIds)
        if (remoteDelete is Result.Failure) return remoteDelete
        return userListLocalDataSource.deleteChecklistEntries(itemIds)
    }

    // MEAL PLAN ENTRY
    override fun observeMealPlanEntriesByListId(
        familyId: String,
        listId: String,
    ): Flow<Result<List<MealPlanEntry>, AppError>> {
        return userListLocalDataSource.observeMealPlanEntriesByListId(familyId, listId)
            .map { entities ->
                appResultOf {
                    entities
                        .map { it.toModel() }
                        .sortedByDescending { it.date }
                }
            }
    }

    override fun syncMealPlanEntriesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>> {
        return syncAccessibleEntriesFromRemote(
            uid = uid,
            familyId = familyId,
            listType = ListType.MEAL_PLANNER,
            entryFlow = userListFirestoreDataSource.familyMealPlanEntriesSnapshotListener(familyId),
            failureMessage = "Meal plan entries sync failed",
            onEmptyEntries = {
                userListLocalDataSource.deleteFamilyMealPlanEntries(familyId)
            },
            onVisibleEntries = { visibleEntries ->
                userListLocalDataSource.updateMealPlanEntries(visibleEntries)
            },
        )
    }

    override fun observeMealPlanEntry(id: String): Flow<Result<MealPlanEntry, AppError>> {
        return userListLocalDataSource.observeMealPlanEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveMealPlanEntry(entry: MealPlanEntry): Result<String, AppError> {
        return userListFirestoreDataSource.saveMealPlanEntry(entry)
    }

    override suspend fun updateMealPlanEntry(entry: MealPlanEntry): Result<Unit, AppError> {
        return userListFirestoreDataSource.updateMealPlanEntry(entry)
    }

    override suspend fun deleteMealPlanEntries(itemIds: List<String>): Result<Unit, AppError> {
        val remoteDelete = userListFirestoreDataSource.deleteMealPlanEntries(itemIds)
        if (remoteDelete is Result.Failure) return remoteDelete
        return userListLocalDataSource.deleteMealPlanEntries(itemIds)
    }

    private fun accessibleListIdsForType(
        sharedLists: List<UserList>,
        privateLists: List<UserList>,
        listType: ListType,
    ): Set<String> {
        return (sharedLists + privateLists)
            .asSequence()
            .filter { it.type == listType }
            .map { it.id }
            .toSet()
    }

    private data class AccessibleUserLists(
        val shared: List<UserList>,
        val private: List<UserList>,
    )

    private fun resolveAccessibleUserLists(
        sharedResult: Result<ListSnapshot<UserList>, AppError>,
        privateResult: Result<ListSnapshot<UserList>, AppError>,
        failureMessage: String,
    ): Result<AccessibleUserLists, AppError> {
        val shared = (sharedResult as? Result.Success)?.data?.items.orEmpty()
        val private = (privateResult as? Result.Success)?.data?.items.orEmpty()

        if (sharedResult !is Result.Success && privateResult !is Result.Success) {
            return Result.Failure(
                AppErrors.unknown(
                    listOfNotNull(
                        (sharedResult as? Result.Failure)?.error,
                        (privateResult as? Result.Failure)?.error,
                    ).joinToString(" | ").ifBlank { failureMessage },
                ),
            )
        }

        return Result.Success(
            AccessibleUserLists(
                shared = shared,
                private = private,
            ),
        )
    }

    private fun <T : ListEntry> syncAccessibleEntriesFromRemote(
        uid: String,
        familyId: String,
        listType: ListType,
        entryFlow: Flow<Result<ListSnapshot<T>, AppError>>,
        failureMessage: String,
        onEmptyEntries: suspend () -> Unit,
        onVisibleEntries: suspend (List<T>) -> Unit,
    ): Flow<Result<Unit, AppError>> {
        return combine(
            userListFirestoreDataSource.familySharedUserListsSnapshotListener(familyId),
            userListFirestoreDataSource.privateUserListsSnapshotListener(familyId, uid),
            entryFlow,
        ) { sharedResult, privateResult, entriesResult ->
            when (val accessibleListsResult = resolveAccessibleUserLists(
                sharedResult = sharedResult,
                privateResult = privateResult,
                failureMessage = failureMessage,
            )) {
                is Result.Failure -> accessibleListsResult
                is Result.Success -> when (entriesResult) {
                    is Result.Success -> appResultOfSuspend {
                        val accessibleListIds = accessibleListIdsForType(
                            accessibleListsResult.data.shared,
                            accessibleListsResult.data.private,
                            listType,
                        )
                        val visibleEntries = entriesResult.data.items.filter { it.listId in accessibleListIds }

                        if (visibleEntries.isEmpty()) {
                            onEmptyEntries()
                        } else {
                            onVisibleEntries(visibleEntries)
                        }
                    }

                    is Result.Failure -> Result.Failure(entriesResult.error)
                }
            }
        }
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

    private fun WishListEntryEntity.toModel() = WishListEntry(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        isPurchased = isPurchased,
        url = url,
        estimatedPriceMinor = estimatedPriceMinor,
        currencyCode = currencyCode,
        priority = priority,
        notes = notes,
    )

    private fun NoteEntryEntity.toModel() = NoteEntry(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        markdownBody = markdownBody,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )

    private fun ChecklistEntryEntity.toModel() = ChecklistEntry(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        isChecked = isChecked,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )

    private fun MealPlanEntryEntity.toModel() = MealPlanEntry(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        date = date,
        recipeId = recipeId,
        customMealName = customMealName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )
}
