package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import kotlin.jvm.Transient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class TipTrackerFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "TipTrackerFirestoreDS"
    }
    fun tipTrackerSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.TIP_TRACKER_TABLE).whereEqualTo("familyId", familyId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(TipItemDto::class.java)?.toDomain(doc.id)
                    } catch (throwable: Throwable) {
                        Log.e(TAG, "Failed parsing tip item ${doc.id}", throwable)
                        null
                    }
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveTip(tip: TipItem): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.TIP_TRACKER_TABLE).add(tip.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun deleteTip(tipId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.TIP_TRACKER_TABLE).document(tipId).delete().await()
        }
    }
}

private data class TipItemDto(
    @DocumentId @Transient
    val id: String? = null,
    val familyId: String? = null,
    @get:PropertyName("item_name")
    val itemName: String? = null,
    @get:PropertyName("last_updated")
    val lastUpdated: Date? = null,
    val amount: Float? = null,
    val currency: String? = null,
    val date: Date? = null,
) {
    fun toDomain(documentId: String): TipItem? {
        val familyIdValue = familyId?.takeIf { it.isNotBlank() } ?: return null
        val itemNameValue = itemName?.takeIf { it.isNotBlank() } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        val amountValue = amount ?: return null
        val currencyValue = currency?.takeIf { it.isNotBlank() } ?: return null
        val dateValue = date ?: return null
        return TipItem(
            id = documentId,
            familyId = familyIdValue,
            itemName = itemNameValue,
            lastUpdated = lastUpdatedValue,
            amount = amountValue,
            currency = currencyValue,
            date = dateValue,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "familyId" to familyId,
        "item_name" to itemName,
        "last_updated" to lastUpdated,
        "amount" to amount,
        "currency" to currency,
        "date" to date,
    )
}

private fun TipItem.toDto(): TipItemDto = TipItemDto(
    id = id,
    familyId = familyId,
    itemName = itemName,
    lastUpdated = lastUpdated,
    amount = amount,
    currency = currency,
    date = date,
)
