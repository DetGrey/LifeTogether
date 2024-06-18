package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import javax.inject.Inject

class RemoteUserRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) : UserRepository {
    override suspend fun login(
        user: User,
    ): AuthResultListener {
        return firebaseAuthDataSource.login(user)
    }

    override suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
        try {
            val signupResult = firebaseAuthDataSource.signUp(user, userInformation)
            return if (signupResult is AuthResultListener.Success) {
                when (val uploadResult = firestoreDataSource.uploadUserInformation(signupResult.userInformation)) {
                    is ResultListener.Success -> signupResult
                    is ResultListener.Failure -> AuthResultListener.Failure(uploadResult.message)
                }
            } else {
                signupResult
            }
        } catch (e: Exception) {
            return AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): AuthResultListener {
        try {
            val currentUserUid = firebaseAuthDataSource.getCurrentUser()
            return if (currentUserUid != null) {
                firestoreDataSource.getUserInformation(currentUserUid)
            } else {
                AuthResultListener.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            return AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun logout(): ResultListener {
        return firebaseAuthDataSource.logout()
    }

    override suspend fun changeName(uid: String, newName: String): ResultListener {
        return firestoreDataSource.changeName(uid, newName)
    }
}
