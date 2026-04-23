package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FamilyFirestoreDataSource @Inject constructor(
    private val userFirestoreDataSource: UserFirestoreDataSource,
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "FamilyFirestoreDS"
    }
    fun familyInformationSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.FAMILIES_TABLE).document(familyId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                val membersData = snapshot.get("members") as? List<Map<String, String>> ?: emptyList()
                val members = membersData.map { FamilyMember(uid = it["uid"], name = it["name"]) }
                trySend(
                    Result.Success(
                        FamilyInformation(
                            familyId = familyId,
                            members = members,
                            imageUrl = snapshot.getString("imageUrl"),
                        ),
                    ),
                ).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun joinFamily(familyId: String, uid: String, name: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()
            @Suppress("UNCHECKED_CAST")
            val members = doc.data?.get("members") as? List<Map<String, String>>
            val updatedMembers = members?.toMutableList() ?: mutableListOf()
            updatedMembers.add(mapOf("uid" to uid, "name" to name))
            db.collection(Constants.FAMILIES_TABLE).document(familyId).update("members", updatedMembers).await()
            Result.Success(Unit)
        }
    }

    suspend fun createNewFamily(uid: String, name: String): Result<String, AppError> {
        return appResultOfSuspend {
            val map = mapOf("members" to listOf(mapOf("uid" to uid, "name" to name)))
            val documentReference = db.collection(Constants.FAMILIES_TABLE).add(map).await()
            documentReference.id
        }
    }

    suspend fun leaveFamily(familyId: String, uid: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()
            @Suppress("UNCHECKED_CAST")
            val members = doc.data?.get("members") as? List<Map<String, String>>
            val updatedMembers = members?.filterNot { it["uid"] == uid }?.toMutableList() ?: mutableListOf()
            db.collection(Constants.FAMILIES_TABLE).document(familyId).update("members", updatedMembers).await()
            Result.Success(Unit)
        }
    }

    suspend fun deleteFamily(familyId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.FAMILIES_TABLE).document(familyId).delete().await()
            val usersRef = db.collection(Constants.USER_TABLE).whereEqualTo("familyId", familyId).get().await()
            val failures = mutableListOf<AppError>()
            for (userDocument in usersRef.documents) {
                val uid = userDocument.id
                val result = userFirestoreDataSource.updateFamilyId(uid, null)
                if (result is Result.Failure) failures.add(result.error)
            }
            if (failures.isNotEmpty()) {
                throw AppErrorThrowable(
                    AppErrors.conflict("Could not remove familyId from all users: $failures")
                )
            }
        }
    }

    suspend fun getFamilyImageUrl(familyId: String): Result<String, AppError> = appResultOfSuspend {
        val doc = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()
        val url = doc.getString("imageUrl")

        url ?: throw AppErrorThrowable(AppErrors.notFound("Family image not found"))
    }

    suspend fun saveFamilyImageUrl(familyId: String, url: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.FAMILIES_TABLE).document(familyId)
                .update(mapOf("imageUrl" to url)).await()
        }
    }

    suspend fun storeFcmToken(uid: String, familyId: String) {
        val fcmToken = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch FCM token", e)
            return
        }
        if (fcmToken.isBlank()) return

        try {
            val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
            val familyDocSnapshot = familyDocRef.get().await()
            if (!familyDocSnapshot.exists()) return

            @Suppress("UNCHECKED_CAST")
            val members = familyDocSnapshot.get("members") as? List<Map<String, Any>> ?: emptyList()
            val updatedMembers = members.map { member ->
                if (member["uid"] == uid) {
                    val currentToken = member["fcmToken"] as? String
                    if (currentToken != fcmToken) member.toMutableMap().apply { put("fcmToken", fcmToken) } else member
                } else {
                    member
                }
            }
            if (updatedMembers != members) {
                familyDocRef.update("members", updatedMembers).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
        }
    }

    suspend fun removeDeviceToken(uid: String, familyId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
            val document = familyDocRef.get().await()
            if (!document.exists()) throw AppErrorThrowable(AppErrors.notFound("Family document not found"))

            @Suppress("UNCHECKED_CAST")
            val members = document.get("members") as? List<Map<String, Any>> ?: emptyList()
            val updatedMembers = members.map { member ->
                if (member["uid"] == uid) member.toMutableMap().apply { remove("fcmToken") } else member
            }
            familyDocRef.update("members", updatedMembers).await()
        }
    }

    suspend fun getFcmTokensFromFamily(familyId: String): List<String>? {
        val familyDoc = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()
        @Suppress("UNCHECKED_CAST")
        val members = familyDoc.data?.get("members") as? List<Map<String, Any>> ?: emptyList()
        val tokens = members.mapNotNull { it["fcmToken"] as? String }
        return tokens.ifEmpty { null }
    }
}
