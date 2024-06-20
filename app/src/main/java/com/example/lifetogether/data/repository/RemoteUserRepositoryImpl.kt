package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirebaseAuthDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class RemoteUserRepositoryImpl @Inject constructor(
    private val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) : UserRepository {
    override suspend fun login(
        user: User,
    ): AuthResultListener {
        println("RemoteUserRepositoryImpl login() init")
        return firebaseAuthDataSource.login(user)
    }

    override suspend fun signUp(
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

    suspend fun getCurrentUser(): Flow<AuthResultListener> {
        println("RemoteUserRepositoryImpl getCurrentUser()")
        return flow {
            try {
                val currentUserUid = firebaseAuthDataSource.getCurrentUserUid()
                println("RemoteUserRepositoryImpl currentUserUid: $currentUserUid")
                if (currentUserUid != null) {
                    emit(firestoreDataSource.getUserInformation(currentUserUid))
                } else {
                    emit(AuthResultListener.Failure("Authentication failed"))
                }
            } catch (e: Exception) {
                emit(AuthResultListener.Failure(e.message ?: "Unknown error"))
            }
        }
    }

    override suspend fun logout(): ResultListener {
        return firebaseAuthDataSource.logout()
    }

    override suspend fun changeName(uid: String, newName: String): ResultListener {
        return firestoreDataSource.changeName(uid, newName)
    }
}
