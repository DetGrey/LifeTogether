package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource {
    suspend fun login(
        user: User,
    ): AuthResultListener {
        try {
            val loginResult = Firebase.auth.signInWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = loginResult.user
            return if (firebaseUser != null) {
                AuthResultListener.Success(
                    UserInformation(uid = firebaseUser.uid),
                )
            } else {
                AuthResultListener.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            return AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
        try {
            val loginResult = Firebase.auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = loginResult.user
            if (firebaseUser != null) {
                val updatedUserInformation = userInformation.copy(uid = firebaseUser.uid)
                return AuthResultListener.Success(updatedUserInformation)
            } else {
                return AuthResultListener.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            return AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    fun getCurrentUserUid(): String? {
        return try {
            Firebase.auth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    fun logout(): ResultListener {
        try {
            Firebase.auth.signOut()
            return ResultListener.Success
        } catch (e: Exception) {
            return ResultListener.Failure("Error: ${e.message}")
        }
    }
}
