package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.logic.GuideParser
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideProgressState
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GuideFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "GuideFirestoreDS"
    }
    fun familySharedGuidesSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.GUIDES_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("visibility", Constants.VISIBILITY_FAMILY)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val guides = snapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                runCatching { GuideParser.parseFirestoreGuide(document.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing family guide ${document.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(guides))).isSuccess
        }
        awaitClose { registration.remove() }
    }

    fun privateGuidesSnapshotListener(familyId: String, uid: String) = callbackFlow {
        val ref = db.collection(Constants.GUIDES_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("visibility", Constants.VISIBILITY_PRIVATE)
            .whereEqualTo("ownerUid", uid)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val guides = snapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                runCatching { GuideParser.parseFirestoreGuide(document.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing private guide ${document.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(ListSnapshot(guides))).isSuccess
        }
        awaitClose { registration.remove() }
    }

    fun guideProgressSnapshotListener(familyId: String, uid: String) = callbackFlow {
        val ref = db.collection(Constants.GUIDE_PROGRESS_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("uid", uid)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val progress = snapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                runCatching { GuideParser.parseGuideProgressMap(document.id, data) }
                    .onFailure { Log.e(TAG, "Failed parsing guide progress ${document.id}", it) }
                    .getOrNull()
            }
            trySend(Result.Success(progress)).isSuccess
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveGuide(guide: Guide): Result<String, AppError> {
        return try {
            val upload = GuideParser.guideToFirestoreMap(guide)
            val doc = db.collection(Constants.GUIDES_TABLE).add(upload).await()
            Result.Success(doc.id)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun updateGuide(guide: Guide): Result<Unit, AppError> {
        return try {
            val id = guide.id ?: return Result.Failure(AppErrors.validation("Missing guide id"))
            val upload = GuideParser.guideToFirestoreMap(guide)
            db.collection(Constants.GUIDES_TABLE).document(id).set(upload, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun deleteGuide(guideId: String): Result<Unit, AppError> {
        return try {
            deleteGuideWithRelatedProgress(guideId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun updateGuideProgress(progress: GuideProgressState): Result<Unit, AppError> {
        return try {
            db.collection(Constants.GUIDE_PROGRESS_TABLE)
                .document(progress.id)
                .set(GuideParser.guideProgressToFirestoreMap(progress), SetOptions.merge())
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    private suspend fun deleteGuideWithRelatedProgress(guideId: String) {
        db.collection(Constants.GUIDES_TABLE).document(guideId).delete().await()
        runCatching {
            val progressRefs = db.collection(Constants.GUIDE_PROGRESS_TABLE)
                .whereEqualTo("guideId", guideId)
                .get()
                .await()
                .documents
                .map { it.reference }
            progressRefs.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { ref -> batch.delete(ref) }
                batch.commit().await()
            }
        }.onFailure {
            Log.w(TAG, "Guide deleted but related guide_progress cleanup failed for guideId=$guideId", it)
        }
    }
}
