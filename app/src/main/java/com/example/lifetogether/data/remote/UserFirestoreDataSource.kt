package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.logic.AppErrorThrowable
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
    fun userInformationSnapshotListener(uid: String) = callbackFlow {
        val ref = db.collection(Constants.USER_TABLE).document(uid)

        val registration = ref.addSnapshotListener { snapshot, error ->
            val result = when {
                error != null -> Result.Failure(AppErrors.fromThrowable(error))
                snapshot != null && snapshot.exists() -> {
                    val data = snapshot.toObject(UserInformation::class.java)
                    if (data != null) Result.Success(data)
                    else Result.Failure(AppErrors.unknown("Mapping error"))
                }
                else -> Result.Failure(AppErrors.notFound("User not found"))
            }

            trySend(result)
        }

        awaitClose { registration.remove() }
    }

    suspend fun fetchUserInformation(uid: String): Result<UserInformation, AppError> = appResultOfSuspend {
        val snapshot = db.collection(Constants.USER_TABLE).document(uid).get().await()
        val userInformation = snapshot.toObject(UserInformation::class.java)
        userInformation ?: throw AppErrorThrowable(AppErrors.notFound("User not found"))
    }

    suspend fun uploadUserInformation(userInformation: UserInformation): Result<Unit, AppError> {
        val uid = userInformation.uid ?: return Result.Failure(AppErrors.authentication("Cannot upload without being logged in"))
        return appResultOfSuspend {
            db.collection(Constants.USER_TABLE).document(uid).set(userInformation).await()
        }
    }

    suspend fun updateFamilyId(uid: String, familyId: String?): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.USER_TABLE).document(uid).update("familyId", familyId).await()
        }
    }

    suspend fun changeName(
        uid: String,
        familyId: String?,
        newName: String,
    ): Result<Unit, AppError> {
        return appResultOfSuspend {
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
        }
    }

    suspend fun getUserImageUrl(uid: String): Result<String, AppError> = appResultOfSuspend {
        val document = db.collection(Constants.USER_TABLE).document(uid).get().await()
        val url = document.getString("imageUrl")
        url ?: throw AppErrorThrowable(AppErrors.notFound("User image not found"))
    }

    suspend fun saveUserImageUrl(uid: String, url: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.USER_TABLE).document(uid).update(mapOf("imageUrl" to url)).await()
        }
    }
}
