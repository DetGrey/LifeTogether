package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.MealPlanEntry
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.lists.WishListPriority
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class UserListFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "UserListFirestoreDS"
    }
    fun familySharedUserListsSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.USER_LISTS_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("visibility", Constants.VISIBILITY_FAMILY)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                runCatching { parseFirestoreUserList(doc.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing family user list ${doc.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun privateUserListsSnapshotListener(familyId: String, uid: String) = callbackFlow {
        val ref = db.collection(Constants.USER_LISTS_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("visibility", Constants.VISIBILITY_PRIVATE)
            .whereEqualTo("ownerUid", uid)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                runCatching { parseFirestoreUserList(doc.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing private user list ${doc.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun familyRoutineListEntriesSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).whereEqualTo("familyId", familyId)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                runCatching { parseFirestoreRoutineListEntry(doc.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing list entry ${doc.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun familyWishListEntriesSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.WISH_LIST_ENTRIES_TABLE).whereEqualTo("familyId", familyId)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                runCatching { parseFirestoreWishListEntry(doc.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing wish list entry ${doc.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun familyNoteEntriesSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.NOTE_LIST_ENTRIES_TABLE).whereEqualTo("familyId", familyId)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                runCatching { parseFirestoreNoteEntry(doc.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing note entry ${doc.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun familyChecklistEntriesSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.CHECKLIST_ENTRIES_TABLE).whereEqualTo("familyId", familyId)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                runCatching { parseFirestoreChecklistEntry(doc.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing checklist entry ${doc.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    fun familyMealPlanEntriesSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.MEAL_PLAN_ENTRIES_TABLE).whereEqualTo("familyId", familyId)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                runCatching { parseFirestoreMealPlanEntry(doc.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing meal plan entry ${doc.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveUserList(userList: UserList): Result<String, AppError> {
        return appResultOfSuspend {
            val id = userList.id.trim()
            if (id.isEmpty()) {
                throw AppErrorThrowable(AppErrors.validation("Missing user list id"))
            }
            db.collection(Constants.USER_LISTS_TABLE).document(id)
                .set(userListToFirestoreMap(userList), SetOptions.merge())
                .await()
            id
        }
    }

    suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE)
                .add(listEntryToFirestoreMap(entry)).await()
            doc.id
        }
    }

    suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing routine list entry id"))
        return appResultOfSuspend {
            db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(id)
                .set(listEntryToFirestoreMap(entry), SetOptions.merge())
                .await()
        }
    }

    suspend fun saveWishListEntry(entry: WishListEntry): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.WISH_LIST_ENTRIES_TABLE)
                .add(wishListEntryToFirestoreMap(entry)).await()
            doc.id
        }
    }

    suspend fun updateWishListEntry(entry: WishListEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing wish list entry id"))
        return appResultOfSuspend {
            db.collection(Constants.WISH_LIST_ENTRIES_TABLE).document(id)
                .set(wishListEntryToFirestoreMap(entry), SetOptions.merge())
                .await()
        }
    }

    suspend fun deleteWishListEntries(itemIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            itemIds.forEach { id ->
                batch.delete(db.collection(Constants.WISH_LIST_ENTRIES_TABLE).document(id))
            }
            batch.commit().await()
        }
    }

    suspend fun saveNoteEntry(entry: NoteEntry): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.NOTE_LIST_ENTRIES_TABLE)
                .add(noteEntryToFirestoreMap(entry)).await()
            doc.id
        }
    }

    suspend fun updateNoteEntry(entry: NoteEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing note entry id"))
        return appResultOfSuspend {
            db.collection(Constants.NOTE_LIST_ENTRIES_TABLE).document(id)
                .set(noteEntryToFirestoreMap(entry), SetOptions.merge())
                .await()
        }
    }

    suspend fun deleteNoteEntries(itemIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            itemIds.forEach { id ->
                batch.delete(db.collection(Constants.NOTE_LIST_ENTRIES_TABLE).document(id))
            }
            batch.commit().await()
        }
    }

    suspend fun saveChecklistEntry(entry: ChecklistEntry): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.CHECKLIST_ENTRIES_TABLE)
                .add(checklistEntryToFirestoreMap(entry)).await()
            doc.id
        }
    }

    suspend fun updateChecklistEntry(entry: ChecklistEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing checklist entry id"))
        return appResultOfSuspend {
            db.collection(Constants.CHECKLIST_ENTRIES_TABLE).document(id)
                .set(checklistEntryToFirestoreMap(entry), SetOptions.merge())
                .await()
        }
    }

    suspend fun deleteChecklistEntries(itemIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            itemIds.forEach { id ->
                batch.delete(db.collection(Constants.CHECKLIST_ENTRIES_TABLE).document(id))
            }
            batch.commit().await()
        }
    }

    suspend fun saveMealPlanEntry(entry: MealPlanEntry): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.MEAL_PLAN_ENTRIES_TABLE)
                .add(mealPlanEntryToFirestoreMap(entry)).await()
            doc.id
        }
    }

    suspend fun updateMealPlanEntry(entry: MealPlanEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing meal plan entry id"))
        return appResultOfSuspend {
            db.collection(Constants.MEAL_PLAN_ENTRIES_TABLE).document(id)
                .set(mealPlanEntryToFirestoreMap(entry), SetOptions.merge())
                .await()
        }
    }

    suspend fun deleteMealPlanEntries(itemIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            itemIds.forEach { id ->
                batch.delete(db.collection(Constants.MEAL_PLAN_ENTRIES_TABLE).document(id))
            }
            batch.commit().await()
        }
    }

    suspend fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            itemIds.forEach { id ->
                batch.delete(db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(id))
            }
            batch.commit().await()
        }
    }

    suspend fun getRoutineListEntryImageUrl(entryId: String): Result<String, AppError> = appResultOfSuspend {
        val doc = db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE)
            .document(entryId)
            .get()
            .await()

        val url = doc.getString("imageUrl")
        url ?: throw AppErrorThrowable(AppErrors.notFound("List entry image not found"))
    }

    suspend fun saveRoutineListEntryImageUrl(entryId: String, url: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(entryId)
                .update(mapOf("imageUrl" to url))
                .await()
        }
    }

    private fun parseFirestoreUserList(documentId: String, data: Map<String, Any?>): UserList {
        fun requiredStr(key: String): String =
            (data[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Missing $key")
        fun date(key: String): Date? = when (val v = data[key]) {
            is Timestamp -> v.toDate()
            is Long -> Date(v)
            else -> null
        }
        fun requiredDate(key: String): Date = date(key) ?: throw IllegalArgumentException("Missing $key")
        fun requiredListType(value: String): com.example.lifetogether.domain.model.lists.ListType {
            return com.example.lifetogether.domain.model.lists.ListType.entries.firstOrNull {
                it.value.equals(value, ignoreCase = true)
            } ?: throw IllegalArgumentException("Invalid type: $value")
        }
        fun requiredVisibility(value: String): Visibility {
            return Visibility.entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid visibility: $value")
        }
        val itemName = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: requiredStr("itemName")
        return UserList(
            id = documentId,
            familyId = requiredStr("familyId"),
            itemName = itemName,
            lastUpdated = requiredDate("lastUpdated"),
            dateCreated = requiredDate("dateCreated"),
            type = requiredListType(requiredStr("type")),
            visibility = requiredVisibility(requiredStr("visibility")),
            ownerUid = requiredStr("ownerUid"),
        )
    }

    private fun parseFirestoreRoutineListEntry(documentId: String, data: Map<String, Any?>): RoutineListEntry? {
        fun str(key: String): String? = (data[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        fun date(key: String): Date? = when (val v = data[key]) {
            is Timestamp -> v.toDate()
            is Long -> Date(v)
            else -> null
        }
        fun intVal(key: String, default: Int = 0) = when (val v = data[key]) {
            is Long -> v.toInt()
            is Int -> v
            else -> default
        }
        @Suppress("UNCHECKED_CAST")
        val weekdaysRaw = (data["weekdays"] as? List<*>)?.mapNotNull {
            when (it) {
                is Long -> it.toInt()
                is Int -> it
                else -> null
            }
        } ?: emptyList()

        val familyId = str("familyId") ?: return null
        val listId = str("listId") ?: return null
        val itemName = str("name") ?: str("itemName") ?: return null
        val lastUpdated = date("lastUpdated") ?: return null
        val dateCreated = date("dateCreated") ?: return null
        val nextDate = date("nextDate") ?: return null
        val recurrenceUnit = RecurrenceUnit.fromValue(str("recurrenceUnit"))
        val interval = intVal("interval", 1)
        if (interval < 1) return null
        if (recurrenceUnit == RecurrenceUnit.WEEKS && weekdaysRaw.isEmpty()) return null

        val baseEntry = RoutineListEntry(
            id = documentId,
            familyId = familyId,
            listId = listId,
            itemName = itemName,
            lastUpdated = lastUpdated,
            dateCreated = dateCreated,
            nextDate = nextDate,
            lastCompletedAt = date("lastCompletedAt"),
            completionCount = intVal("completionCount"),
            recurrenceUnit = recurrenceUnit,
            interval = interval,
            weekdays = weekdaysRaw,
            imageUrl = data["imageUrl"] as? String,
        )

        return baseEntry
    }

    private fun parseFirestoreWishListEntry(documentId: String, data: Map<String, Any?>): WishListEntry {
        fun str(key: String) = (data[key] as? String).orEmpty()
        fun date(key: String): Date? = when (val v = data[key]) {
            is Timestamp -> v.toDate()
            is Long -> Date(v)
            else -> null
        }
        fun boolVal(key: String, default: Boolean = false) = when (val v = data[key]) {
            is Boolean -> v
            is Long -> v != 0L
            else -> default
        }
        fun longVal(key: String): Long? = when (val v = data[key]) {
            is Long -> v
            is Int -> v.toLong()
            else -> null
        }

        return WishListEntry(
            id = documentId,
            familyId = str("familyId"),
            listId = str("listId"),
            itemName = str("name").ifBlank { str("itemName") },
            lastUpdated = date("lastUpdated") ?: Date(),
            dateCreated = date("dateCreated") ?: Date(),
            isPurchased = boolVal("isPurchased"),
            url = data["url"] as? String,
            estimatedPriceMinor = longVal("estimatedPriceMinor"),
            currencyCode = data["currencyCode"] as? String,
            priority = WishListPriority.fromValue(data["priority"] as? String),
            notes = data["notes"] as? String,
        )
    }

    private fun parseFirestoreNoteEntry(documentId: String, data: Map<String, Any?>): NoteEntry {
        fun str(key: String) = (data[key] as? String).orEmpty()
        fun date(key: String): Date? = when (val v = data[key]) {
            is Timestamp -> v.toDate()
            is Long -> Date(v)
            else -> null
        }

        return NoteEntry(
            id = documentId,
            familyId = str("familyId"),
            listId = str("listId"),
            itemName = str("name").ifBlank { str("itemName") },
            markdownBody = str("markdownBody").ifBlank { str("body") },
            lastUpdated = date("lastUpdated") ?: Date(),
            dateCreated = date("dateCreated") ?: Date(),
        )
    }

    private fun parseFirestoreChecklistEntry(documentId: String, data: Map<String, Any?>): ChecklistEntry {
        fun str(key: String) = (data[key] as? String).orEmpty()
        fun date(key: String): Date? = when (val v = data[key]) {
            is Timestamp -> v.toDate()
            is Long -> Date(v)
            else -> null
        }
        fun boolVal(key: String, default: Boolean = false) = when (val v = data[key]) {
            is Boolean -> v
            is Long -> v != 0L
            else -> default
        }

        return ChecklistEntry(
            id = documentId,
            familyId = str("familyId"),
            listId = str("listId"),
            itemName = str("name").ifBlank { str("itemName") },
            isChecked = boolVal("isChecked"),
            lastUpdated = date("lastUpdated") ?: Date(),
            dateCreated = date("dateCreated") ?: Date(),
        )
    }

    private fun parseFirestoreMealPlanEntry(documentId: String, data: Map<String, Any?>): MealPlanEntry {
        fun str(key: String) = (data[key] as? String).orEmpty()
        fun date(key: String): Date? = when (val v = data[key]) {
            is Timestamp -> v.toDate()
            is Long -> Date(v)
            else -> null
        }

        return MealPlanEntry(
            id = documentId,
            familyId = str("familyId"),
            listId = str("listId"),
            itemName = str("name").ifBlank { str("itemName") },
            date = str("date"),
            recipeId = data["recipeId"] as? String,
            customMealName = data["customMealName"] as? String,
            lastUpdated = date("lastUpdated") ?: Date(),
            dateCreated = date("dateCreated") ?: Date(),
        )
    }

    private fun userListToFirestoreMap(list: UserList): Map<String, Any?> = mapOf(
        "familyId" to list.familyId,
        "ownerUid" to list.ownerUid,
        "visibility" to list.visibility.value,
        "name" to list.itemName,
        "type" to list.type.value,
        "lastUpdated" to list.lastUpdated,
        "dateCreated" to list.dateCreated,
    )

    private fun listEntryToFirestoreMap(entry: RoutineListEntry): Map<String, Any?> = mapOf(
        "familyId" to entry.familyId,
        "listId" to entry.listId,
        "name" to entry.itemName,
        "lastUpdated" to entry.lastUpdated,
        "dateCreated" to entry.dateCreated,
        "nextDate" to entry.nextDate,
        "lastCompletedAt" to entry.lastCompletedAt,
        "completionCount" to entry.completionCount,
        "recurrenceUnit" to entry.recurrenceUnit.value,
        "interval" to entry.interval,
        "weekdays" to entry.weekdays,
        "imageUrl" to entry.imageUrl,
    )

    private fun wishListEntryToFirestoreMap(entry: WishListEntry): Map<String, Any?> = mapOf(
        "familyId" to entry.familyId,
        "listId" to entry.listId,
        "name" to entry.itemName,
        "lastUpdated" to entry.lastUpdated,
        "dateCreated" to entry.dateCreated,
        "isPurchased" to entry.isPurchased,
        "url" to entry.url,
        "estimatedPriceMinor" to entry.estimatedPriceMinor,
        "currencyCode" to entry.currencyCode,
        "priority" to entry.priority.value,
        "notes" to entry.notes,
    )

    private fun noteEntryToFirestoreMap(entry: NoteEntry): Map<String, Any?> = mapOf(
        "familyId" to entry.familyId,
        "listId" to entry.listId,
        "name" to entry.itemName,
        "markdownBody" to entry.markdownBody,
        "lastUpdated" to entry.lastUpdated,
        "dateCreated" to entry.dateCreated,
    )

    private fun checklistEntryToFirestoreMap(entry: ChecklistEntry): Map<String, Any?> = mapOf(
        "familyId" to entry.familyId,
        "listId" to entry.listId,
        "name" to entry.itemName,
        "isChecked" to entry.isChecked,
        "lastUpdated" to entry.lastUpdated,
        "dateCreated" to entry.dateCreated,
    )

    private fun mealPlanEntryToFirestoreMap(entry: MealPlanEntry): Map<String, Any?> = mapOf(
        "familyId" to entry.familyId,
        "listId" to entry.listId,
        "name" to entry.itemName,
        "date" to entry.date,
        "recipeId" to entry.recipeId,
        "customMealName" to entry.customMealName,
        "lastUpdated" to entry.lastUpdated,
        "dateCreated" to entry.dateCreated,
    )
}
