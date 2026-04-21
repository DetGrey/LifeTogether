package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
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
                trySend(Result.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.toObject(TipItem::class.java)?.copy(id = doc.id) }
                        .onFailure { Log.e(TAG, "Failed parsing tip item ${doc.id}", it) }
                        .getOrNull()
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveTip(tip: TipItem): Result<String, AppError> {
        return try {
            val doc = db.collection(Constants.TIP_TRACKER_TABLE).add(tip).await()
            Result.Success(doc.id)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteTip(tipId: String): Result<Unit, AppError> {
        return try {
            db.collection(Constants.TIP_TRACKER_TABLE).document(tipId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }
}
