package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthDataSource@Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
) {
    suspend fun login(
        user: User,
    ): Result<UserInformation, String> {
        println("FirebaseAuthDataSource login()")
        try {
            val loginResult = Firebase.auth.signInWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = loginResult.user
            return if (firebaseUser != null) {
                Result.Success(
                    UserInformation(uid = firebaseUser.uid),
                )
            } else {
                Result.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            println("FirebaseAuthDataSource login() error: ${e.message}")
            return Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): Result<UserInformation, String> {
        println("FirebaseAuthDataSource signUp()")
        try {
            val signupResult = Firebase.auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = signupResult.user
            println("signupResult: $signupResult")
            println("firebaseUser: $firebaseUser")
            if (firebaseUser != null) {
                val updatedUserInformation = userInformation.copy(uid = firebaseUser.uid)
                return Result.Success(updatedUserInformation)
            } else {
                return Result.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            return Result.Failure("Error: ${e.message}")
        }
    }

    fun authStateListener(): Flow<Result<UserInformation, String>> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val currentUser = auth.currentUser
            if (currentUser != null) {
                trySend(Result.Success(UserInformation(uid = currentUser.uid)))
            } else {
                trySend(Result.Failure("Authentication failed"))
            }
        }

        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)

        awaitClose {
            FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        }
    }

    fun currentUserUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun logout(
        uid: String,
        familyId: String?,
    ): Result<Unit, String> {
        try {
            FirebaseAuth.getInstance().signOut()
            println("datasource logout result: ${FirebaseAuth.getInstance().currentUser}")
            if (familyId != null) {
                firestoreDataSource.removeDeviceToken(uid, familyId)
            }
            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Failure("Error: ${e.message}")
        }
    }
}
