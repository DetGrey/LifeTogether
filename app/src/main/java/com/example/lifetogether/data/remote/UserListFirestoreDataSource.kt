package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
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

    suspend fun saveUserList(userList: UserList): Result<String, AppError> {
        return try {
            val doc = db.collection(Constants.USER_LISTS_TABLE).add(userListToFirestoreMap(userList)).await()
            Result.Success(doc.id)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, AppError> {
        return try {
            val doc = db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).add(listEntryToFirestoreMap(entry)).await()
            Result.Success(doc.id)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, AppError> {
        val id = entry.id ?: return Result.Failure(AppErrors.validation("Missing routine list entry id"))
        return try {
            db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(id)
                .set(listEntryToFirestoreMap(entry), SetOptions.merge())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun deleteRoutineListEntries(itemIds: List<String>): Result<Unit, AppError> {
        return try {
            val batch = db.batch()
            itemIds.forEach { id ->
                batch.delete(db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(id))
            }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun getRoutineListEntryImageUrl(entryId: String): Result<String, AppError> {
        return try {
            val doc = db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(entryId).get().await()
            val url = doc.getString("imageUrl")
            if (url != null) Result.Success(url) else Result.Failure(AppErrors.notFound("List entry image not found"))
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun saveRoutineListEntryImageUrl(entryId: String, url: String): Result<Unit, AppError> {
        return try {
            db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(entryId)
                .update(mapOf("imageUrl" to url))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    private fun parseFirestoreUserList(documentId: String, data: Map<String, Any?>): UserList {
        fun str(key: String) = (data[key] as? String).orEmpty()
        fun date(key: String): Date? = when (val v = data[key]) {
            is Timestamp -> v.toDate()
            is Long -> Date(v)
            else -> null
        }
        return UserList(
            id = documentId,
            familyId = str("familyId"),
            itemName = str("name").ifBlank { str("itemName") },
            lastUpdated = date("lastUpdated") ?: Date(),
            dateCreated = date("dateCreated") ?: Date(),
            type = ListType.fromValue(str("type")),
            visibility = Visibility.fromValue(str("visibility")),
            ownerUid = str("ownerUid"),
        )
    }

    private fun parseFirestoreRoutineListEntry(documentId: String, data: Map<String, Any?>): RoutineListEntry {
        fun str(key: String) = (data[key] as? String).orEmpty()
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

        return RoutineListEntry(
            id = documentId,
            familyId = str("familyId"),
            listId = str("listId"),
            itemName = str("name").ifBlank { str("itemName") },
            lastUpdated = date("lastUpdated") ?: Date(),
            dateCreated = date("dateCreated") ?: Date(),
            nextDate = date("nextDate"),
            lastCompletedAt = date("lastCompletedAt"),
            completionCount = intVal("completionCount"),
            recurrenceUnit = RecurrenceUnit.fromValue(str("recurrenceUnit")),
            interval = intVal("interval", 1),
            weekdays = weekdaysRaw,
            imageUrl = data["imageUrl"] as? String,
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
}
