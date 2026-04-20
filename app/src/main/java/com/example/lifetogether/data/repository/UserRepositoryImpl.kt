package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.SessionCleanupLocalDataSource
import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
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
    private val firestoreDataSource: FirestoreDataSource,
) : UserRepository, SessionUserRepository {

    fun getFamilyInformation(familyId: String): Flow<Result<FamilyInformation, String>> {
        // Get family information (without members)
        val familyInformationFlow: Flow<Result<FamilyInformation, String>> = userLocalDataSource.getFamilyInformation(familyId).map { user ->
            try {
                println("LocalUserRepositoryImpl getFamilyInformation user: $user")

                // Initial FamilyInformation without members
                Result.Success(
                    FamilyInformation(
                        familyId = user.familyId,
                    ),
                )
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown error")
            }
        }

        // Get family members
        val familyMembersFlow = userLocalDataSource.getFamilyMembers(familyId).map { list ->
            list.map { familyMember ->
                try {
                    println("LocalUserRepositoryImpl getFamilyInformation familyMember: $familyMember")

                    FamilyMember(
                        uid = familyMember.uid,
                        name = familyMember.name,
                    )
                } catch (e: Exception) {
                    println("Error fetching family members: ${e.message}")
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

    override fun removeSavedUserInformation(): Result<Unit, String> {
        return sessionCleanupLocalDataSource.clearSessionTables()
    }
    // ---------- REMOTE
    suspend fun login(
        user: User,
    ): Result<UserInformation, String> {
        println("RemoteUserRepositoryImpl login()")
        return firebaseAuthDataSource.login(user)
    }

    suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): Result<UserInformation, String> {
        println("RemoteUserRepositoryImpl signUp()")
        try {
            val signupResult = firebaseAuthDataSource.signUp(user, userInformation)
            println("RemoteUserRepositoryImpl signupResult: $signupResult")
            return if (signupResult is Result.Success) {
                when (val uploadResult = firestoreDataSource.uploadUserInformation(signupResult.data)) {
                    is Result.Success -> {
                        println("RemoteUserRepositoryImpl: uploadResult $uploadResult")
                        signupResult
                    }
                    is Result.Failure -> {
                        println("RemoteUserRepositoryImpl: uploadResult $uploadResult")
                        Result.Failure(uploadResult.error)
                    }
                }
            } else {
                signupResult
            }
        } catch (e: Exception) {
            return Result.Failure("Error: ${e.message}")
        }
    }

    override suspend fun logout(
        uid: String,
        familyId: String?,
    ): Result<Unit, String> {
        return firebaseAuthDataSource.logout(uid, familyId)
    }

    override suspend fun fetchUserInformation(uid: String): Result<UserInformation, String> {
        return firestoreDataSource.fetchUserInformation(uid)
    }

    override fun observeUserInformation(uid: String): Flow<Result<UserInformation, String>> {
        return firestoreDataSource.userInformationSnapshotListener(uid)
    }

    override suspend fun changeName(
        uid: String,
        familyId: String?,
        newName: String,
    ): Result<Unit, String> {
        return when (val result = firestoreDataSource.changeName(uid, familyId, newName)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    suspend fun joinFamily(
        familyId: String,
        uid: String,
        name: String,
    ): Result<Unit, String> {
        println("RemoteUserRepositoryImpl joinFamily()")
        when (val result = firestoreDataSource.joinFamily(familyId, uid, name)) {
            is Result.Success -> {
                val updateResult = firestoreDataSource.updateFamilyId(uid, familyId)
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
    ): Result<Unit, String> {
        println("RemoteUserRepositoryImpl createNewFamily()")
        when (val result = firestoreDataSource.createNewFamily(uid, name)) {
            is Result.Success -> {
                val updateResult = firestoreDataSource.updateFamilyId(uid, result.data)
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
    ): Result<Unit, String> {
        println("RemoteUserRepositoryImpl leaveFamily()")
        when (val result = firestoreDataSource.leaveFamily(familyId, uid)) {
            is Result.Success -> {
                val updateResult = firestoreDataSource.updateFamilyId(uid, null)
                return updateResult
            }
            is Result.Failure -> {
                return Result.Failure(result.error)
            }
        }
    }

    suspend fun deleteFamily(
        familyId: String,
    ): Result<Unit, String> {
        println("RemoteUserRepositoryImpl deleteFamily()")
        return firestoreDataSource.deleteFamily(familyId)
    }

    override suspend fun storeFcmToken(
        uid: String,
        familyId: String,
    ): Result<Unit, String> {
        return runCatching {
            firestoreDataSource.storeFcmToken(uid, familyId)
            Result.Success(Unit)
        }.getOrElse { error ->
            Result.Failure(error.message ?: "Failed to store FCM token")
        }
    }

    override suspend fun fetchFcmTokens(
        familyId: String,
    ): List<String>? {
        return firestoreDataSource.getFcmTokensFromFamily(familyId)
    }
}
