package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UserFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "UserFirestoreDS"
    }
    fun userInformationSnapshotListener(uid: String) = callbackFlow {
        val ref = db.collection(Constants.USER_TABLE).document(uid)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }
            val userInformation = snapshot?.toObject(UserInformation::class.java)
            if (userInformation != null) trySend(Result.Success(userInformation)).isSuccess
            else trySend(Result.Failure("User not found")).isSuccess
        }
        awaitClose { registration.remove() }
    }

    suspend fun fetchUserInformation(uid: String): Result<UserInformation, AppError> {
        return try {
            val snapshot = db.collection(Constants.USER_TABLE).document(uid).get().await()
            val userInformation = snapshot.toObject(UserInformation::class.java)
            if (userInformation != null) Result.Success(userInformation) else Result.Failure("User not found")
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun uploadUserInformation(userInformation: UserInformation): Result<Unit, AppError> {
        return try {
            val uid = userInformation.uid ?: return Result.Failure("Cannot upload without being logged in")
            db.collection(Constants.USER_TABLE).document(uid).set(userInformation).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun updateFamilyId(uid: String, familyId: String?): Result<Unit, AppError> {
        return try {
            db.collection(Constants.USER_TABLE).document(uid).update("familyId", familyId).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun changeName(
        uid: String,
        familyId: String?,
        newName: String,
    ): Result<Unit, AppError> {
        return try {
            db.collection(Constants.USER_TABLE).document(uid).update("name", newName).await()
            if (familyId != null) {
                val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
                val familySnapshot = familyDocRef.get().await()
                if (familySnapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val members = familySnapshot.get("members") as? List<Map<String, String>> ?: emptyList()
                    val updatedMembers = members.map { member ->
                        if (member["uid"] == uid) member.toMutableMap().apply { this["name"] = newName } else member
                    }
                    familyDocRef.update("members", updatedMembers).await()
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun getUserImageUrl(uid: String): Result<String, AppError> {
        return try {
            val document = db.collection(Constants.USER_TABLE).document(uid).get().await()
            val url = document.getString("imageUrl")
            if (url != null) Result.Success(url) else Result.Failure(AppError.NotFound("User image not found"))
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun saveUserImageUrl(uid: String, url: String): Result<Unit, AppError> {
        return try {
            db.collection(Constants.USER_TABLE).document(uid).update(mapOf("imageUrl" to url)).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }
}
