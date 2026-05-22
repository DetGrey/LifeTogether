package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.Transient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class UserFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "UserFirestoreDS"
    }

    fun userInformationSnapshotListener(uid: String) = callbackFlow {
        val ref = db.collection(Constants.USER_TABLE).document(uid)

        val registration = ref.addSnapshotListener { snapshot, error ->
            val result = when {
                error != null -> Result.Failure(AppErrors.fromThrowable(error))
                snapshot != null && snapshot.exists() -> {
                    val data = mapFirestoreDocument(
                        tag = TAG,
                        collectionName = Constants.USER_TABLE,
                        entityName = "UserInformation",
                        document = snapshot,
                    ) {
                        it.toObject(UserInformationDto::class.java)?.toDomain(it.id)
                    }
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
        val userInformation = mapFirestoreDocument(
            tag = TAG,
            collectionName = Constants.USER_TABLE,
            entityName = "UserInformation",
            document = snapshot,
        ) {
            it.toObject(UserInformationDto::class.java)?.toDomain(it.id)
        }
        userInformation ?: throw AppErrorThrowable(AppErrors.notFound("User not found"))
    }

    suspend fun uploadUserInformation(userInformation: UserInformation): Result<Unit, AppError> {
        val uid = userInformation.uid
        if (uid.isBlank()) {
            return Result.Failure(AppErrors.authentication("Cannot upload without being logged in"))
        }
        return appResultOfSuspend {
            db.collection(Constants.USER_TABLE).document(uid).set(userInformation.toDto().toFirestoreMap()).await()
        }
    }

    suspend fun updateFamilyId(uid: String, familyId: String?, lastUpdated: Date): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.USER_TABLE).document(uid)
                .update(
                    mapOf(
                        "familyId" to familyId,
                        "lastUpdated" to lastUpdated,
                    ),
                )
                .await()
        }
    }

    suspend fun changeName(
        uid: String,
        familyId: String?,
        newName: String,
        lastUpdated: Date,
    ): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.USER_TABLE).document(uid)
                .update(
                    mapOf(
                        "name" to newName,
                        "lastUpdated" to lastUpdated,
                    ),
                )
                .await()
            if (familyId != null) {
                val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
                val familySnapshot = familyDocRef.get().await()
                if (familySnapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val members = familySnapshot.get("members") as? List<Map<String, String>> ?: emptyList()
                    val updatedMembers = members.map { member ->
                        if (member["uid"] == uid) member.toMutableMap().apply { this["name"] = newName } else member
                    }
                    familyDocRef.update(
                        mapOf(
                            "members" to updatedMembers,
                            "lastUpdated" to lastUpdated,
                        ),
                    ).await()
                }
            }
        }
    }

    suspend fun getUserImageUrl(uid: String): Result<String, AppError> = appResultOfSuspend {
        val document = db.collection(Constants.USER_TABLE).document(uid).get().await()
        val url = document.getString("imageUrl")
        url ?: throw AppErrorThrowable(AppErrors.notFound("User image not found"))
    }

    suspend fun saveUserImageUrl(uid: String, url: String, lastUpdated: Date = Date()): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.USER_TABLE).document(uid)
                .update(
                    mapOf(
                        "imageUrl" to url,
                        "lastUpdated" to lastUpdated,
                    ),
                )
                .await()
        }
    }
}

private data class UserInformationDto(
    @DocumentId @Transient
    val uid: String? = null,
    val email: String? = null,
    val name: String? = null,
    val lastUpdated: Date? = null,
    val birthday: Date? = null,
    val familyId: String? = null,
    val imageUrl: String? = null,
) {
    fun toDomain(documentId: String): UserInformation? {
        val uidValue = uid?.takeIf { it.isNotBlank() } ?: documentId.takeIf { it.isNotBlank() } ?: return null
        val emailValue = email?.takeIf { it.isNotBlank() } ?: return null
        val nameValue = name?.takeIf { it.isNotBlank() } ?: return null
        return UserInformation(
            uid = uidValue,
            email = emailValue,
            name = nameValue,
            lastUpdated = lastUpdated ?: Date(0),
            birthday = birthday,
            familyId = familyId,
            imageUrl = imageUrl,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "email" to email,
        "name" to name,
        "lastUpdated" to lastUpdated,
        "birthday" to birthday,
        "familyId" to familyId,
        "imageUrl" to imageUrl,
    )
}

private fun UserInformation.toDto(): UserInformationDto = UserInformationDto(
    uid = uid,
    email = email,
    name = name,
    lastUpdated = lastUpdated,
    birthday = birthday,
    familyId = familyId,
    imageUrl = imageUrl,
)
