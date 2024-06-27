package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthDataSource {
    suspend fun login(
        user: User,
    ): AuthResultListener {
        println("FirebaseAuthDataSource login()")
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
            println("FirebaseAuthDataSource login() error: ${e.message}")
            return AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
        println("FirebaseAuthDataSource signUp()")
        try {
            val signupResult = Firebase.auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = signupResult.user
            println("signupResult: $signupResult")
            println("firebaseUser: $firebaseUser")
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
        try {
            val uid = Firebase.auth.currentUser?.uid
            println("FirebaseAuthDataSource getCurrentUserUid: $uid")
            return uid
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun authStateListener(): Flow<AuthResultListener> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val currentUser = auth.currentUser
            if (currentUser != null) {
                trySend(AuthResultListener.Success(UserInformation(uid = currentUser.uid)))
            } else {
                trySend(AuthResultListener.Failure("Authentication failed"))
            }
        }

        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

        awaitClose {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
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
