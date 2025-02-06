package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import javax.inject.Inject

class RemoteUserRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) : UserRepository {
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

    override fun logout(): ResultListener {
        return firebaseAuthDataSource.logout()
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
}
