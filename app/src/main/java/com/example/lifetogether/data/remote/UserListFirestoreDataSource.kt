package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.lists.ChecklistEntry
import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.lists.NoteEntry
import com.example.lifetogether.domain.model.lists.RecurrenceUnit
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.lists.WishListEntry
import com.example.lifetogether.domain.model.lists.WishListPriority
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.DocumentId
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
            val items = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.USER_LISTS_TABLE,
                entityName = "UserList",
                documents = snapshot.documents,
            ) { doc ->
                doc.toObject(UserListDto::class.java)?.toDomain(doc.id)
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
            val items = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.USER_LISTS_TABLE,
                entityName = "UserList",
                documents = snapshot.documents,
            ) { doc ->
                doc.toObject(UserListDto::class.java)?.toDomain(doc.id)
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
            val items = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.ROUTINE_LIST_ENTRIES_TABLE,
                entityName = "RoutineListEntry",
                documents = snapshot.documents,
            ) { doc ->
                doc.toObject(RoutineListEntryDto::class.java)?.toDomain(doc.id)
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
            val items = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.WISH_LIST_ENTRIES_TABLE,
                entityName = "WishListEntry",
                documents = snapshot.documents,
            ) { doc ->
                doc.toObject(WishListEntryDto::class.java)?.toDomain(doc.id)
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
            val items = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.NOTE_LIST_ENTRIES_TABLE,
                entityName = "NoteEntry",
                documents = snapshot.documents,
            ) { doc ->
                doc.toObject(NoteEntryDto::class.java)?.toDomain(doc.id)
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
            val items = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.CHECKLIST_ENTRIES_TABLE,
                entityName = "ChecklistEntry",
                documents = snapshot.documents,
            ) { doc ->
                doc.toObject(ChecklistEntryDto::class.java)?.toDomain(doc.id)
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
                .set(userList.toDto().toFirestoreMap(), SetOptions.merge())
                .await()
            id
        }
    }

    suspend fun saveRoutineListEntry(entry: RoutineListEntry): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE)
                .add(entry.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun updateRoutineListEntry(entry: RoutineListEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing routine list entry id"))
        return appResultOfSuspend {
            db.collection(Constants.ROUTINE_LIST_ENTRIES_TABLE).document(id)
                .set(entry.toDto().toFirestoreMap(), SetOptions.merge())
                .await()
        }
    }

    suspend fun saveWishListEntry(entry: WishListEntry): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.WISH_LIST_ENTRIES_TABLE)
                .add(entry.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun updateWishListEntry(entry: WishListEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing wish list entry id"))
        return appResultOfSuspend {
            db.collection(Constants.WISH_LIST_ENTRIES_TABLE).document(id)
                .set(entry.toDto().toFirestoreMap(), SetOptions.merge())
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
                .add(entry.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun updateNoteEntry(entry: NoteEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing note entry id"))
        return appResultOfSuspend {
            db.collection(Constants.NOTE_LIST_ENTRIES_TABLE).document(id)
                .set(entry.toDto().toFirestoreMap(), SetOptions.merge())
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
                .add(entry.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun updateChecklistEntry(entry: ChecklistEntry): Result<Unit, AppError> {
        val id = entry.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing checklist entry id"))
        return appResultOfSuspend {
            db.collection(Constants.CHECKLIST_ENTRIES_TABLE).document(id)
                .set(entry.toDto().toFirestoreMap(), SetOptions.merge())
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
    suspend fun deleteLegacyMealPlannerUserLists(familyId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            val userListDocs = db.collection(Constants.USER_LISTS_TABLE)
                .whereEqualTo("familyId", familyId)
                .whereIn("type", listOf("meal_planner", "meal_plan"))
                .get()
                .await()

            val entryDocs = db.collection("list_entries_meal_plan")
                .whereEqualTo("familyId", familyId)
                .get()
                .await()

            val batch = db.batch()
            userListDocs.documents.forEach { batch.delete(it.reference) }
            entryDocs.documents.forEach { batch.delete(it.reference) }
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

    private data class UserListDto(
        @DocumentId @Transient
        val id: String? = null,
        val familyId: String? = null,
        val name: String? = null,
        val itemName: String? = null,
        val lastUpdated: Date? = null,
        val dateCreated: Date? = null,
        val type: String? = null,
        val visibility: String? = null,
        val ownerUid: String? = null,
    ) {
        fun toDomain(documentId: String): UserList? {
            val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val itemNameValue = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: itemName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val typeValue = ListType.entries.firstOrNull { listType ->
                type?.trim()?.takeIf { it.isNotEmpty() }?.let { listType.value.equals(it, ignoreCase = true) }
                    ?: false
            }
                ?: return null
            val visibilityValue = Visibility.entries.firstOrNull { listVisibility ->
                visibility?.trim()?.takeIf { it.isNotEmpty() }?.let { listVisibility.value.equals(it, ignoreCase = true) }
                    ?: false
            }
                ?: return null

            return UserList(
                id = documentId,
                familyId = familyIdValue,
                itemName = itemNameValue,
                lastUpdated = lastUpdated ?: return null,
                dateCreated = dateCreated ?: return null,
                type = typeValue,
                visibility = visibilityValue,
                ownerUid = ownerUid?.trim()?.takeIf { it.isNotEmpty() } ?: return null,
            )
        }
    }

    private data class RoutineListEntryDto(
        @DocumentId @Transient
        val id: String? = null,
        val familyId: String? = null,
        val listId: String? = null,
        val name: String? = null,
        val itemName: String? = null,
        val lastUpdated: Date? = null,
        val dateCreated: Date? = null,
        val nextDate: Date? = null,
        val lastCompletedAt: Date? = null,
        val completionCount: Int? = null,
        val recurrenceUnit: String? = null,
        val interval: Int? = null,
        val weekdays: List<Long>? = null,
        val imageUrl: String? = null,
    ) {
        fun toDomain(documentId: String): RoutineListEntry? {
            val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val listIdValue = listId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val itemNameValue = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: itemName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val lastUpdatedValue = lastUpdated ?: return null
            val dateCreatedValue = dateCreated ?: return null
            val nextDateValue = nextDate ?: return null
            val recurrenceUnitValue = RecurrenceUnit.entries.firstOrNull {
                it.value.equals(recurrenceUnit, ignoreCase = true)
            } ?: return null
            val intervalValue = interval ?: 1
            if (intervalValue < 1) return null
            val weekdaysValue = weekdays?.map { it.toInt() } ?: emptyList()
            if (recurrenceUnitValue == RecurrenceUnit.WEEKS && weekdaysValue.isEmpty()) return null

            return RoutineListEntry(
                id = documentId,
                familyId = familyIdValue,
                listId = listIdValue,
                itemName = itemNameValue,
                lastUpdated = lastUpdatedValue,
                dateCreated = dateCreatedValue,
                nextDate = nextDateValue,
                lastCompletedAt = lastCompletedAt,
                completionCount = completionCount ?: 0,
                recurrenceUnit = recurrenceUnitValue,
                interval = intervalValue,
                weekdays = weekdaysValue,
                imageUrl = imageUrl,
            )
        }
    }

    private data class WishListEntryDto(
        @DocumentId @Transient
        val id: String? = null,
        val familyId: String? = null,
        val listId: String? = null,
        val name: String? = null,
        val itemName: String? = null,
        val lastUpdated: Date? = null,
        val dateCreated: Date? = null,
        val isPurchased: Boolean? = null,
        val url: String? = null,
        val price: Double? = null,
        val currencyCode: String? = null,
        val priority: String? = null,
        val notes: String? = null,
    ) {
        fun toDomain(documentId: String): WishListEntry? {
            val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val listIdValue = listId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val itemNameValue = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: itemName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val lastUpdatedValue = lastUpdated ?: return null
            val dateCreatedValue = dateCreated ?: return null
            val priorityValue = WishListPriority.entries.firstOrNull {
                it.value.equals(priority, ignoreCase = true)
            } ?: return null

            return WishListEntry(
                id = documentId,
                familyId = familyIdValue,
                listId = listIdValue,
                itemName = itemNameValue,
                lastUpdated = lastUpdatedValue,
                dateCreated = dateCreatedValue,
                isPurchased = isPurchased ?: false,
                url = url,
                price = price,
                currencyCode = currencyCode,
                priority = priorityValue,
                notes = notes,
            )
        }
    }

    private data class NoteEntryDto(
        @DocumentId @Transient
        val id: String? = null,
        val familyId: String? = null,
        val listId: String? = null,
        val name: String? = null,
        val itemName: String? = null,
        val body: String? = null,
        val lastUpdated: Date? = null,
        val dateCreated: Date? = null,
    ) {
        fun toDomain(documentId: String): NoteEntry? {
            val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val listIdValue = listId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val itemNameValue = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: itemName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val bodyValue = body?.trim() ?: ""
            val lastUpdatedValue = lastUpdated ?: return null
            val dateCreatedValue = dateCreated ?: return null

            return NoteEntry(
                id = documentId,
                familyId = familyIdValue,
                listId = listIdValue,
                itemName = itemNameValue,
                body = bodyValue,
                lastUpdated = lastUpdatedValue,
                dateCreated = dateCreatedValue,
            )
        }
    }

    private data class ChecklistEntryDto(
        @DocumentId @Transient
        val id: String? = null,
        val familyId: String? = null,
        val listId: String? = null,
        val name: String? = null,
        val itemName: String? = null,
        val isChecked: Boolean? = null,
        val lastUpdated: Date? = null,
        val dateCreated: Date? = null,
    ) {
        fun toDomain(documentId: String): ChecklistEntry? {
            val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val listIdValue = listId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val itemNameValue = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: itemName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val lastUpdatedValue = lastUpdated ?: return null
            val dateCreatedValue = dateCreated ?: return null

            return ChecklistEntry(
                id = documentId,
                familyId = familyIdValue,
                listId = listIdValue,
                itemName = itemNameValue,
                isChecked = isChecked ?: false,
                lastUpdated = lastUpdatedValue,
                dateCreated = dateCreatedValue,
            )
        }
    }
    private data class UserListWriteDto(
        val familyId: String?,
        val ownerUid: String?,
        val visibility: String?,
        val name: String?,
        val type: String?,
        val lastUpdated: Date?,
        val dateCreated: Date?,
    ) {
        fun toFirestoreMap(): Map<String, Any?> = mapOf(
            "familyId" to familyId,
            "ownerUid" to ownerUid,
            "visibility" to visibility,
            "name" to name,
            "type" to type,
            "lastUpdated" to lastUpdated,
            "dateCreated" to dateCreated,
        )
    }

    private data class RoutineListEntryWriteDto(
        val familyId: String?,
        val listId: String?,
        val name: String?,
        val lastUpdated: Date?,
        val dateCreated: Date?,
        val nextDate: Date?,
        val lastCompletedAt: Date?,
        val completionCount: Int?,
        val recurrenceUnit: String?,
        val interval: Int?,
        val weekdays: List<Int>?,
        val imageUrl: String?,
    ) {
        fun toFirestoreMap(): Map<String, Any?> = mapOf(
            "familyId" to familyId,
            "listId" to listId,
            "name" to name,
            "lastUpdated" to lastUpdated,
            "dateCreated" to dateCreated,
            "nextDate" to nextDate,
            "lastCompletedAt" to lastCompletedAt,
            "completionCount" to completionCount,
            "recurrenceUnit" to recurrenceUnit,
            "interval" to interval,
            "weekdays" to weekdays,
            "imageUrl" to imageUrl,
        )
    }

    private data class WishListEntryWriteDto(
        val familyId: String?,
        val listId: String?,
        val name: String?,
        val lastUpdated: Date?,
        val dateCreated: Date?,
        val isPurchased: Boolean?,
        val url: String?,
        val price: Double?,
        val currencyCode: String?,
        val priority: String?,
        val notes: String?,
    ) {
        fun toFirestoreMap(): Map<String, Any?> = mapOf(
            "familyId" to familyId,
            "listId" to listId,
            "name" to name,
            "lastUpdated" to lastUpdated,
            "dateCreated" to dateCreated,
            "isPurchased" to isPurchased,
            "url" to url,
            "price" to price,
            "currencyCode" to currencyCode,
            "priority" to priority,
            "notes" to notes,
        )
    }

    private data class NoteEntryWriteDto(
        val familyId: String?,
        val listId: String?,
        val name: String?,
        val body: String?,
        val lastUpdated: Date?,
        val dateCreated: Date?,
    ) {
        fun toFirestoreMap(): Map<String, Any?> = mapOf(
            "familyId" to familyId,
            "listId" to listId,
            "name" to name,
            "body" to body,
            "lastUpdated" to lastUpdated,
            "dateCreated" to dateCreated,
        )
    }

    private data class ChecklistEntryWriteDto(
        val familyId: String?,
        val listId: String?,
        val name: String?,
        val isChecked: Boolean?,
        val lastUpdated: Date?,
        val dateCreated: Date?,
    ) {
        fun toFirestoreMap(): Map<String, Any?> = mapOf(
            "familyId" to familyId,
            "listId" to listId,
            "name" to name,
            "isChecked" to isChecked,
            "lastUpdated" to lastUpdated,
            "dateCreated" to dateCreated,
        )
    }
    private fun UserList.toDto(): UserListWriteDto = UserListWriteDto(
        familyId = familyId,
        ownerUid = ownerUid,
        visibility = visibility.value,
        name = itemName,
        type = type.value,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )

    private fun RoutineListEntry.toDto(): RoutineListEntryWriteDto = RoutineListEntryWriteDto(
        familyId = familyId,
        listId = listId,
        name = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        nextDate = nextDate,
        lastCompletedAt = lastCompletedAt,
        completionCount = completionCount,
        recurrenceUnit = recurrenceUnit.value,
        interval = interval,
        weekdays = weekdays,
        imageUrl = imageUrl,
    )

    private fun WishListEntry.toDto(): WishListEntryWriteDto = WishListEntryWriteDto(
        familyId = familyId,
        listId = listId,
        name = itemName,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
        isPurchased = isPurchased,
        url = url,
        price = price,
        currencyCode = currencyCode,
        priority = priority.value,
        notes = notes,
    )

    private fun NoteEntry.toDto(): NoteEntryWriteDto = NoteEntryWriteDto(
        familyId = familyId,
        listId = listId,
        name = itemName,
        body = body,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )

    private fun ChecklistEntry.toDto(): ChecklistEntryWriteDto = ChecklistEntryWriteDto(
        familyId = familyId,
        listId = listId,
        name = itemName,
        isChecked = isChecked,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )
}
