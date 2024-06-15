package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class UserRepositoryImpl : UserRepository {

    override suspend fun login(
        user: User,
    ): AuthResultListener {
        return try {
            val loginResult = Firebase.auth.signInWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = loginResult.user
            if (firebaseUser != null) {
                AuthResultListener.Success(
                    UserInformation(uid = firebaseUser.uid),
                )
            } else {
                AuthResultListener.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun signUp(
        user: User,
    ): AuthResultListener {
        return try {
            val loginResult = Firebase.auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = loginResult.user
            if (firebaseUser != null) {
                AuthResultListener.Success(
                    UserInformation(uid = firebaseUser.uid),
                )
            } else {
                AuthResultListener.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): AuthResultListener {
        return try {
            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser != null) {
                AuthResultListener.Success(
                    UserInformation(uid = firebaseUser.uid),
                )
            } else {
                AuthResultListener.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            AuthResultListener.Failure("Error: ${e.message}")
        }
    }
}
