package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.logic.appResultOf

import com.example.lifetogether.data.local.dao.ChecklistEntriesDao
import com.example.lifetogether.data.local.dao.MealPlanEntriesDao
import com.example.lifetogether.data.local.dao.NoteEntriesDao
import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.dao.RoutineListsDao
import com.example.lifetogether.data.local.dao.UserListsDao
import com.example.lifetogether.data.local.dao.WishListsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.ChecklistEntryEntity
import com.example.lifetogether.data.model.MealPlanEntryEntity
import com.example.lifetogether.data.model.NoteEntryEntity
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.data.model.WishListEntryEntity
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserListLocalDataSource @Inject constructor(
    private val userListsDao: UserListsDao,
    private val routineListsDao: RoutineListsDao,
    private val wishListsDao: WishListsDao,
    private val noteEntriesDao: NoteEntriesDao,
    private val checklistEntriesDao: ChecklistEntriesDao,
    private val mealPlanEntriesDao: MealPlanEntriesDao,
) {
    fun observeUserLists(familyId: String): Flow<List<UserListEntity>> {
        return userListsDao.getItems(familyId)
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
            itemsToDelete.forEach { list ->
                deleteChildEntriesForList(list.id, list.type)
            }
            userListsDao.deleteItems(itemsToDelete.map { it.id })
        }
    }

    fun deleteFamilyUserLists(familyId: String) {
        routineListsDao.deleteFamilyItems(familyId)
        wishListsDao.deleteFamilyItems(familyId)
        noteEntriesDao.deleteFamilyItems(familyId)
        checklistEntriesDao.deleteFamilyItems(familyId)
        mealPlanEntriesDao.deleteFamilyItems(familyId)
        userListsDao.deleteFamilyItems(familyId)
    }

    private fun deleteChildEntriesForList(listId: String, listType: ListType) {
        when (listType) {
            ListType.ROUTINE -> routineListsDao.deleteByListIds(listOf(listId))
            ListType.WISH_LIST -> wishListsDao.deleteByListIds(listOf(listId))
            ListType.NOTES -> noteEntriesDao.deleteByListIds(listOf(listId))
            ListType.CHECKLIST -> checklistEntriesDao.deleteByListIds(listOf(listId))
            ListType.MEAL_PLANNER -> mealPlanEntriesDao.deleteByListIds(listOf(listId))
        }
    }

    fun observeRoutineListEntry(id: String): Flow<RoutineListEntryEntity> {
        return routineListsDao.getItemById(id)
    }

    fun observeRoutineListEntriesByListId(familyId: String, listId: String): Flow<List<RoutineListEntryEntity>> {
        return routineListsDao.getItemsByListId(familyId, listId)
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

    fun observeRoutineImageByteArray(entryId: String): Flow<ByteArray?> =
        routineListsDao.observeImageByteArray(entryId)

    fun observeWishListEntriesByListId(familyId: String, listId: String): Flow<List<WishListEntryEntity>> {
        return wishListsDao.getItemsByListId(familyId, listId)
    }

    fun observeWishListEntry(id: String): Flow<WishListEntryEntity> {
        return wishListsDao.getItemById(id)
    }

    suspend fun updateWishListEntries(items: List<WishListEntry>) {
        if (items.isEmpty()) return
        val currentItems = wishListsDao.getItems(items.first().familyId).first()
        val entities = items.map { it.toEntity() }
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
        wishListsDao.updateItems(itemsToUpdate)
        if (itemsToDelete.isNotEmpty()) {
            wishListsDao.deleteItems(itemsToDelete.map { it.id })
        }
    }

    fun deleteFamilyWishListEntries(familyId: String) {
        wishListsDao.deleteFamilyItems(familyId)
    }

    fun deleteWishListEntries(itemIds: List<String>): Result<Unit, AppError> = appResultOf {
        wishListsDao.deleteItems(itemIds)
    }

    fun observeNoteEntriesByListId(familyId: String, listId: String): Flow<List<NoteEntryEntity>> {
        return noteEntriesDao.getItemsByListId(familyId, listId)
    }

    fun observeNoteEntry(id: String): Flow<NoteEntryEntity> {
        return noteEntriesDao.getItemById(id)
    }

    suspend fun updateNoteEntries(items: List<NoteEntry>) {
        if (items.isEmpty()) return
        val currentItems = noteEntriesDao.getItems(items.first().familyId).first()
        val entities = items.map { it.toEntity() }
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
        noteEntriesDao.updateItems(itemsToUpdate)
        if (itemsToDelete.isNotEmpty()) {
            noteEntriesDao.deleteItems(itemsToDelete.map { it.id })
        }
    }

    fun deleteFamilyNoteEntries(familyId: String) {
        noteEntriesDao.deleteFamilyItems(familyId)
    }

    fun deleteNoteEntries(itemIds: List<String>): Result<Unit, AppError> = appResultOf {
        noteEntriesDao.deleteItems(itemIds)
    }

    fun observeChecklistEntriesByListId(familyId: String, listId: String): Flow<List<ChecklistEntryEntity>> {
        return checklistEntriesDao.getItemsByListId(familyId, listId)
    }

    fun observeChecklistEntry(id: String): Flow<ChecklistEntryEntity> {
        return checklistEntriesDao.getItemById(id)
    }

    suspend fun updateChecklistEntries(items: List<ChecklistEntry>) {
        if (items.isEmpty()) return
        val currentItems = checklistEntriesDao.getItems(items.first().familyId).first()
        val entities = items.map { it.toEntity() }
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
        checklistEntriesDao.updateItems(itemsToUpdate)
        if (itemsToDelete.isNotEmpty()) {
            checklistEntriesDao.deleteItems(itemsToDelete.map { it.id })
        }
    }

    fun deleteFamilyChecklistEntries(familyId: String) {
        checklistEntriesDao.deleteFamilyItems(familyId)
    }

    fun deleteChecklistEntries(itemIds: List<String>): Result<Unit, AppError> = appResultOf {
        checklistEntriesDao.deleteItems(itemIds)
    }

    fun observeMealPlanEntriesByListId(familyId: String, listId: String): Flow<List<MealPlanEntryEntity>> {
        return mealPlanEntriesDao.getItemsByListId(familyId, listId)
    }

    fun observeMealPlanEntry(id: String): Flow<MealPlanEntryEntity> {
        return mealPlanEntriesDao.getItemById(id)
    }

    suspend fun updateMealPlanEntries(items: List<MealPlanEntry>) {
        if (items.isEmpty()) return
        val currentItems = mealPlanEntriesDao.getItems(items.first().familyId).first()
        val entities = items.map { it.toEntity() }
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
        mealPlanEntriesDao.updateItems(itemsToUpdate)
        if (itemsToDelete.isNotEmpty()) {
            mealPlanEntriesDao.deleteItems(itemsToDelete.map { it.id })
        }
    }

    fun deleteFamilyMealPlanEntries(familyId: String) {
        mealPlanEntriesDao.deleteFamilyItems(familyId)
    }

    fun deleteMealPlanEntries(itemIds: List<String>): Result<Unit, AppError> = appResultOf {
        mealPlanEntriesDao.deleteItems(itemIds)
    }

    private fun RoutineListEntry.toEntity() = RoutineListEntryEntity(
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
        imageData = null,
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
        estimatedPriceMinor = estimatedPriceMinor,
        currencyCode = currencyCode,
        priority = priority,
        notes = notes,
    )

    private fun NoteEntry.toEntity() = NoteEntryEntity(
        id = id,
        familyId = familyId,
        listId = listId,
        itemName = itemName,
        markdownBody = markdownBody,
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

    private fun MealPlanEntry.toEntity() = MealPlanEntryEntity(
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
