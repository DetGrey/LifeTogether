package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.SessionCleanupLocalDataSource
import com.example.lifetogether.data.local.source.UserLocalDataSource
import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.listener.FamilyInformationResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
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

    fun getFamilyInformation(familyId: String): Flow<FamilyInformationResultListener> {
        // Get family information (without members)
        val familyInformationFlow: Flow<FamilyInformationResultListener> = userLocalDataSource.getFamilyInformation(familyId).map { user ->
            try {
                println("LocalUserRepositoryImpl getFamilyInformation user: $user")

                // Initial FamilyInformation without members
                FamilyInformationResultListener.Success(
                    FamilyInformation(
                        familyId = user.familyId,
                    ),
                )
            } catch (e: Exception) {
                FamilyInformationResultListener.Failure(e.message ?: "Unknown error")
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
                is FamilyInformationResultListener.Success -> {
                    // Add the family members to the family information
                    val validMembers = familyMembers.filterIsInstance<FamilyMember>()
                    val updatedFamilyInfo = familyInfo.familyInformation.copy(members = validMembers)
                    FamilyInformationResultListener.Success(updatedFamilyInfo)
                }
                is FamilyInformationResultListener.Failure -> {
                    familyInfo
                }
            }
        }
    }

    override fun removeSavedUserInformation(): ResultListener {
        return sessionCleanupLocalDataSource.clearSessionTables()
    }
    // ---------- REMOTE
    suspend fun login(
        user: User,
    ): AuthResultListener {
        println("RemoteUserRepositoryImpl login()")
        return firebaseAuthDataSource.login(user)
    }

    suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
        println("RemoteUserRepositoryImpl signUp()")
        try {
            val signupResult = firebaseAuthDataSource.signUp(user, userInformation)
            println("RemoteUserRepositoryImpl signupResult: $signupResult")
            return if (signupResult is AuthResultListener.Success) {
                when (val uploadResult = firestoreDataSource.uploadUserInformation(signupResult.userInformation)) {
                    is ResultListener.Success -> {
                        println("RemoteUserRepositoryImpl: uploadResult $uploadResult")
                        signupResult
                    }
                    is ResultListener.Failure -> {
                        println("RemoteUserRepositoryImpl: uploadResult $uploadResult")
                        AuthResultListener.Failure(uploadResult.message)
                    }
                }
            } else {
                signupResult
            }
        } catch (e: Exception) {
            return AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun logout(
        uid: String,
        familyId: String?,
    ): ResultListener {
        return firebaseAuthDataSource.logout(uid, familyId)
    }

    override suspend fun fetchUserInformation(uid: String): AuthResultListener {
        return firestoreDataSource.fetchUserInformation(uid)
    }

    override fun observeUserInformation(uid: String): Flow<AuthResultListener> {
        return firestoreDataSource.userInformationSnapshotListener(uid)
    }

    suspend fun changeName(
        uid: String,
        familyId: String?,
        newName: String,
    ): ResultListener {
        return firestoreDataSource.changeName(uid, familyId, newName)
    }

    suspend fun joinFamily(
        familyId: String,
        uid: String,
        name: String,
    ): ResultListener {
        println("RemoteUserRepositoryImpl joinFamily()")
        when (val result = firestoreDataSource.joinFamily(familyId, uid, name)) {
            is ResultListener.Success -> {
                val updateResult = firestoreDataSource.updateFamilyId(uid, familyId)
                return updateResult
            }
            is ResultListener.Failure -> {
                return ResultListener.Failure(result.message)
            }
        }
    }

    suspend fun createNewFamily(
        uid: String,
        name: String,
    ): ResultListener {
        println("RemoteUserRepositoryImpl createNewFamily()")
        when (val result = firestoreDataSource.createNewFamily(uid, name)) {
            is StringResultListener.Success -> {
                val updateResult = firestoreDataSource.updateFamilyId(uid, result.string)
                return updateResult
            }
            is StringResultListener.Failure -> {
                return ResultListener.Failure(result.message)
            }
        }
    }

    suspend fun leaveFamily(
        familyId: String,
        uid: String,
    ): ResultListener {
        println("RemoteUserRepositoryImpl leaveFamily()")
        when (val result = firestoreDataSource.leaveFamily(familyId, uid)) {
            is ResultListener.Success -> {
                val updateResult = firestoreDataSource.updateFamilyId(uid, null)
                return updateResult
            }
            is ResultListener.Failure -> {
                return ResultListener.Failure(result.message)
            }
        }
    }

    suspend fun deleteFamily(
        familyId: String,
    ): ResultListener {
        println("RemoteUserRepositoryImpl deleteFamily()")
        return firestoreDataSource.deleteFamily(familyId)
    }

    suspend fun storeFcmToken(
        uid: String,
        familyId: String,
    ): ResultListener {
        firestoreDataSource.storeFcmToken(uid, familyId)
        return ResultListener.Success // TODO this is temp!
    }

    suspend fun fetchFcmTokens(
        familyId: String,
    ): List<String>? {
        return firestoreDataSource.getFcmTokensFromFamily(familyId)
    }
}
