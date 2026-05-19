package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.data.local.source.UserListLocalDataSource
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.model.ChecklistEntryEntity
import com.example.lifetogether.data.model.NoteEntryEntity
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.data.model.WishListEntryEntity
import com.example.lifetogether.data.remote.UserListFirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.ListEntry
import com.example.lifetogether.domain.model.lists.ListType
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
    override suspend fun deleteLegacyMealPlannerUserLists(familyId: String): Result<Unit, AppError> {
        return userListFirestoreDataSource.deleteLegacyMealPlannerUserLists(familyId)
    }

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
                        userListLocalDataSource.updateUserLists(merged.map { it.toEntity() })
                    }
                }
            }
        }
    }

    override suspend fun saveUserList(userList: UserList): Result<String, AppError> {
        userListLocalDataSource.upsertUserList(userList.toEntity())
        return when (val result = userListFirestoreDataSource.saveUserList(userList)) {
            is Result.Success -> Result.Success(userList.id)
            is Result.Failure -> {
                userListLocalDataSource.deleteUserList(userList.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateUserList(userList: UserList): Result<Unit, AppError> {
        val oldEntity = userListLocalDataSource.getUserListOnce(userList.id)
            ?: return Result.Failure(AppErrors.notFound("List not found"))
        userListLocalDataSource.upsertUserList(userList.toEntity())
        return when (val result = userListFirestoreDataSource.saveUserList(userList)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                userListLocalDataSource.upsertUserList(oldEntity)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteUserList(listId: String): Result<Unit, AppError> {
        val oldEntity = userListLocalDataSource.getUserListOnce(listId)
            ?: return Result.Failure(AppErrors.notFound("List not found"))
        userListLocalDataSource.deleteUserListWithEntries(listId, oldEntity.type)
        return when (val result = userListFirestoreDataSource.deleteUserList(listId, oldEntity.type)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                userListLocalDataSource.upsertUserList(oldEntity)
                Result.Failure(result.error)
            }
        }
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
                val currentEntriesById = userListLocalDataSource.getRoutineEntriesOnce(familyId).associateBy { it.id }
                val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                for (entry in visibleEntries) {
                    val imageUrl = entry.imageUrl
                    val currentEntry = currentEntriesById[entry.id]
                    val shouldDownloadImage = imageUrl != null && (
                        currentEntry?.imageUrl != imageUrl || currentEntry.imageData == null
                    )
                    val byteArrayResult = if (shouldDownloadImage) {
                        storageDataSource.fetchImageByteArray(imageUrl)
                    } else {
                        null
                    }
                    if (byteArrayResult is Result.Success) {
                        byteArrays[entry.id] = byteArrayResult.data
                    }
                }
                userListLocalDataSource.updateRoutineListEntries(
                    visibleEntries.map { it.toEntity() },
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
        userListLocalDataSource.upsertRoutineEntry(entry.toEntity())
        return when (val result = userListFirestoreDataSource.saveRoutineListEntry(entry)) {
            is Result.Success -> Result.Success(entry.id)
            is Result.Failure -> {
                userListLocalDataSource.deleteRoutineListEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, AppError> {
        val oldEntity = userListLocalDataSource.getRoutineEntryOnce(entry.id)
        userListLocalDataSource.upsertRoutineEntry(entry.toEntity(oldEntity?.imageData, oldEntity?.imageUrl))
        return when (val result = userListFirestoreDataSource.updateRoutineListEntry(entry)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) userListLocalDataSource.upsertRoutineEntry(oldEntity)
                else userListLocalDataSource.deleteRoutineListEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, AppError> {
        val oldEntities = itemIds.mapNotNull { userListLocalDataSource.getRoutineEntryOnce(it) }
        userListLocalDataSource.deleteRoutineListEntries(itemIds)
        return when (val result = userListFirestoreDataSource.deleteRoutineListEntries(itemIds)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                oldEntities.forEach { userListLocalDataSource.upsertRoutineEntry(it) }
                Result.Failure(result.error)
            }
        }
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
                userListLocalDataSource.updateWishListEntries(visibleEntries.map { it.toEntity() })
            },
        )
    }

    override fun observeWishListEntry(id: String): Flow<Result<WishListEntry, AppError>> {
        return userListLocalDataSource.observeWishListEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveWishListEntry(entry: WishListEntry): Result<String, AppError> {
        userListLocalDataSource.upsertWishEntry(entry.toEntity())
        return when (val result = userListFirestoreDataSource.saveWishListEntry(entry)) {
            is Result.Success -> Result.Success(entry.id)
            is Result.Failure -> {
                userListLocalDataSource.deleteWishListEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateWishListEntry(entry: WishListEntry): Result<Unit, AppError> {
        val oldEntity = userListLocalDataSource.getWishEntryOnce(entry.id)
        userListLocalDataSource.upsertWishEntry(entry.toEntity())
        return when (val result = userListFirestoreDataSource.updateWishListEntry(entry)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) userListLocalDataSource.upsertWishEntry(oldEntity)
                else userListLocalDataSource.deleteWishListEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteWishListEntries(itemIds: List<String>): Result<Unit, AppError> {
        val oldEntities = itemIds.mapNotNull { userListLocalDataSource.getWishEntryOnce(it) }
        userListLocalDataSource.deleteWishListEntries(itemIds)
        return when (val result = userListFirestoreDataSource.deleteWishListEntries(itemIds)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                oldEntities.forEach { userListLocalDataSource.upsertWishEntry(it) }
                Result.Failure(result.error)
            }
        }
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
                userListLocalDataSource.updateNoteEntries(visibleEntries.map { it.toEntity() })
            },
        )
    }

    override fun observeNoteEntry(id: String): Flow<Result<NoteEntry, AppError>> {
        return userListLocalDataSource.observeNoteEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveNoteEntry(entry: NoteEntry): Result<String, AppError> {
        userListLocalDataSource.upsertNoteEntry(entry.toEntity())
        return when (val result = userListFirestoreDataSource.saveNoteEntry(entry)) {
            is Result.Success -> Result.Success(entry.id)
            is Result.Failure -> {
                userListLocalDataSource.deleteNoteEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateNoteEntry(entry: NoteEntry): Result<Unit, AppError> {
        val oldEntity = userListLocalDataSource.getNoteEntryOnce(entry.id)
        userListLocalDataSource.upsertNoteEntry(entry.toEntity())
        return when (val result = userListFirestoreDataSource.updateNoteEntry(entry)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) userListLocalDataSource.upsertNoteEntry(oldEntity)
                else userListLocalDataSource.deleteNoteEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteNoteEntries(itemIds: List<String>): Result<Unit, AppError> {
        val oldEntities = itemIds.mapNotNull { userListLocalDataSource.getNoteEntryOnce(it) }
        userListLocalDataSource.deleteNoteEntries(itemIds)
        return when (val result = userListFirestoreDataSource.deleteNoteEntries(itemIds)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                oldEntities.forEach { userListLocalDataSource.upsertNoteEntry(it) }
                Result.Failure(result.error)
            }
        }
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
                userListLocalDataSource.updateChecklistEntries(visibleEntries.map { it.toEntity() })
            },
        )
    }

    override fun observeChecklistEntry(id: String): Flow<Result<ChecklistEntry, AppError>> {
        return userListLocalDataSource.observeChecklistEntry(id)
            .map { entity -> appResultOf { entity.toModel() } }
    }

    override suspend fun saveChecklistEntry(entry: ChecklistEntry): Result<String, AppError> {
        userListLocalDataSource.upsertChecklistEntry(entry.toEntity())
        return when (val result = userListFirestoreDataSource.saveChecklistEntry(entry)) {
            is Result.Success -> Result.Success(entry.id)
            is Result.Failure -> {
                userListLocalDataSource.deleteChecklistEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateChecklistEntry(entry: ChecklistEntry): Result<Unit, AppError> {
        val oldEntity = userListLocalDataSource.getChecklistEntryOnce(entry.id)
        userListLocalDataSource.upsertChecklistEntry(entry.toEntity())
        return when (val result = userListFirestoreDataSource.updateChecklistEntry(entry)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) userListLocalDataSource.upsertChecklistEntry(oldEntity)
                else userListLocalDataSource.deleteChecklistEntries(listOf(entry.id))
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteChecklistEntries(itemIds: List<String>): Result<Unit, AppError> {
        val oldEntities = itemIds.mapNotNull { userListLocalDataSource.getChecklistEntryOnce(it) }
        userListLocalDataSource.deleteChecklistEntries(itemIds)
        return when (val result = userListFirestoreDataSource.deleteChecklistEntries(itemIds)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                oldEntities.forEach { userListLocalDataSource.upsertChecklistEntry(it) }
                Result.Failure(result.error)
            }
        }
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
        imageUrl = imageUrl,
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
        price = price,
        currencyCode = currencyCode,
        priority = priority,
        notes = notes,
    )

    private fun NoteEntryEntity.toModel() = NoteEntry(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        body = body,
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

    private fun RoutineListEntry.toEntity(
        imageData: ByteArray? = null,
        imageUrl: String? = null,
    ) = RoutineListEntryEntity(
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
        imageData = imageData,
        imageUrl = imageUrl ?: this.imageUrl,
    )

    private fun WishListEntry.toEntity() = WishListEntryEntity(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        isPurchased = isPurchased,
        url = url,
        price = price,
        currencyCode = currencyCode,
        priority = priority,
        notes = notes,
    )

    private fun NoteEntry.toEntity() = NoteEntryEntity(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        body = body,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )

    private fun ChecklistEntry.toEntity() = ChecklistEntryEntity(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        isChecked = isChecked,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )

    private fun UserList.toEntity() = UserListEntity(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        type = type,
        visibility = visibility,
        ownerUid = ownerUid,
    )
}
