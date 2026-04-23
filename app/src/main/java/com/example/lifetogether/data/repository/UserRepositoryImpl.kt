package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.result.AppError
import android.util.Log
import com.example.lifetogether.data.local.source.SessionCleanupLocalDataSource
import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FamilyFirestoreDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.UserFirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.repository.SessionUserRepository
import com.example.lifetogether.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userLocalDataSource: UserLocalDataSource,
    private val sessionCleanupLocalDataSource: SessionCleanupLocalDataSource,
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val userFirestoreDataSource: UserFirestoreDataSource,
    private val familyFirestoreDataSource: FamilyFirestoreDataSource,
    private val storageDataSource: StorageDataSource,
) : UserRepository, SessionUserRepository {
    private companion object {
        const val TAG = "UserRepositoryImpl"
    }

    fun observeFamilyInformation(familyId: String): Flow<Result<FamilyInformation, AppError>> {
        // Get family information (without members)
        val familyInformationFlow: Flow<Result<FamilyInformation, AppError>> = userLocalDataSource.observeFamilyInformation(familyId).map { user ->
            appResultOf {
                Log.d(TAG, "Mapping family information from local user record")

                // Initial FamilyInformation without members
                FamilyInformation(
                    familyId = user.familyId,
                )
            }
        }

        // Get family members
        val familyMembersFlow = userLocalDataSource.observeFamilyMembers(familyId).map { list ->
            list.map { familyMember ->
                try {
                    Log.d(TAG, "Mapping local family member")

                    FamilyMember(
                        uid = familyMember.uid,
                        name = familyMember.name,
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed mapping family member: ${e.message}", e)
                    emptyList<FamilyMember>()
                }
            }
        }

        // Combine both flows
        return familyInformationFlow.combine(familyMembersFlow) { familyInfo, familyMembers ->
            when (familyInfo) {
                is Result.Success -> {
                    // Add the family members to the family information
                    val validMembers = familyMembers.filterIsInstance<FamilyMember>()
                    val updatedFamilyInfo = familyInfo.data.copy(members = validMembers)
                    Result.Success(updatedFamilyInfo)
                }
                is Result.Failure -> {
                    familyInfo
                }
            }
        }
    }

    override fun removeSavedUserInformation(): Result<Unit, AppError> {
        return sessionCleanupLocalDataSource.clearSessionTables()
    }
    // ---------- REMOTE
    override suspend fun login(
        user: User,
    ): Result<UserInformation, AppError> {
        Log.d(TAG, "login start")
        return firebaseAuthDataSource.login(user)
    }

    override suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): Result<UserInformation, AppError> {
        Log.d(TAG, "signUp start")
        return appResultOfSuspend {
            val signupResult = firebaseAuthDataSource.signUp(user, userInformation)
            Log.d(TAG, "signUp auth response received")
            val signedUpUser = when (signupResult) {
                is Result.Success -> signupResult.data
                is Result.Failure -> throw AppErrorThrowable(signupResult.error)
            }
            when (val uploadResult = userFirestoreDataSource.uploadUserInformation(signedUpUser)) {
                is Result.Success -> {
                    Log.d(TAG, "signUp user info upload succeeded")
                    signedUpUser
                }
                is Result.Failure -> {
                    Log.e(TAG, "signUp user info upload failed: ${uploadResult.error}")
                    throw AppErrorThrowable(uploadResult.error)
                }
            }
        }
    }

    override suspend fun logout(
        uid: String,
        familyId: String?,
    ): Result<Unit, AppError> {
        return firebaseAuthDataSource.logout(uid, familyId)
    }

    override suspend fun fetchUserInformation(uid: String): Result<UserInformation, AppError> {
        return userFirestoreDataSource.fetchUserInformation(uid)
    }

    override fun observeUserInformation(uid: String): Flow<Result<UserInformation, AppError>> {
        return userFirestoreDataSource.userInformationSnapshotListener(uid)
    }

    override suspend fun changeName(
        uid: String,
        familyId: String?,
        newName: String,
    ): Result<Unit, AppError> {
        return when (val result = userFirestoreDataSource.changeName(uid, familyId, newName)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    override fun syncUserInformationFromRemote(uid: String): Flow<Result<Unit, AppError>> {
        return userFirestoreDataSource.userInformationSnapshotListener(uid).map { result ->
            when (result) {
                is Result.Success -> appResultOfSuspend {
                    val hasExistingImage = result.data.uid?.let { uidValue ->
                        userLocalDataSource.userHasProfileImage(uidValue)
                    } ?: false

                    if (!hasExistingImage) {
                        val byteArrayResult = result.data.imageUrl?.let { url ->
                            storageDataSource.fetchImageByteArray(url)
                        }
                        when (byteArrayResult) {
                            is Result.Success -> userLocalDataSource.updateUserInformation(result.data, byteArrayResult.data)
                            is Result.Failure -> userLocalDataSource.updateUserInformation(result.data)
                            null -> userLocalDataSource.updateUserInformation(result.data)
                        }
                    }
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    fun syncFamilyInformationFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return familyFirestoreDataSource.familyInformationSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> appResultOfSuspend {
                    val hasExistingImage = result.data.familyId?.let { familyIdValue ->
                        userLocalDataSource.familyHasImage(familyIdValue)
                    } ?: false

                    if (!hasExistingImage) {
                        val byteArrayResult = result.data.imageUrl?.let { url ->
                            storageDataSource.fetchImageByteArray(url)
                        }
                        when (byteArrayResult) {
                            is Result.Success -> userLocalDataSource.updateFamilyInformation(result.data, byteArrayResult.data)
                            is Result.Failure -> userLocalDataSource.updateFamilyInformation(result.data)
                            null -> userLocalDataSource.updateFamilyInformation(result.data)
                        }
                    }
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    suspend fun joinFamily(
        familyId: String,
        uid: String,
        name: String,
    ): Result<Unit, AppError> {
        Log.d(TAG, "joinFamily start")
        when (val result = familyFirestoreDataSource.joinFamily(familyId, uid, name)) {
            is Result.Success -> {
                val updateResult = userFirestoreDataSource.updateFamilyId(uid, familyId)
                return updateResult
            }
            is Result.Failure -> {
                return Result.Failure(result.error)
            }
        }
    }

    suspend fun createNewFamily(
        uid: String,
        name: String,
    ): Result<Unit, AppError> {
        Log.d(TAG, "createNewFamily start")
        when (val result = familyFirestoreDataSource.createNewFamily(uid, name)) {
            is Result.Success -> {
                val updateResult = userFirestoreDataSource.updateFamilyId(uid, result.data)
                return updateResult
            }
            is Result.Failure -> {
                return Result.Failure(result.error)
            }
        }
    }

    suspend fun leaveFamily(
        familyId: String,
        uid: String,
    ): Result<Unit, AppError> {
        Log.d(TAG, "leaveFamily start")
        when (val result = familyFirestoreDataSource.leaveFamily(familyId, uid)) {
            is Result.Success -> {
                val updateResult = userFirestoreDataSource.updateFamilyId(uid, null)
                return updateResult
            }
            is Result.Failure -> {
                return Result.Failure(result.error)
            }
        }
    }

    suspend fun deleteFamily(
        familyId: String,
    ): Result<Unit, AppError> {
        Log.d(TAG, "deleteFamily start")
        return familyFirestoreDataSource.deleteFamily(familyId)
    }

    override suspend fun storeFcmToken(
        uid: String,
        familyId: String,
    ): Result<Unit, AppError> {
        return appResultOfSuspend {
            familyFirestoreDataSource.storeFcmToken(uid, familyId)
        }
    }

    override suspend fun fetchFcmTokens(
        familyId: String,
    ): List<String>? {
        return familyFirestoreDataSource.getFcmTokensFromFamily(familyId)
    }
}
