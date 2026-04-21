package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

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
    private val familyFirestoreDataSource: FamilyFirestoreDataSource,
) {
    suspend fun login(
        user: User,
    ): Result<UserInformation, AppError> {
        println("FirebaseAuthDataSource login()")
        return appResultOfSuspend {
            val loginResult = Firebase.auth.signInWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = loginResult.user
            if (firebaseUser != null) {
                UserInformation(uid = firebaseUser.uid)
            } else {
                throw IllegalStateException("Authentication failed")
            }
        }
    }

    suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): Result<UserInformation, AppError> {
        println("FirebaseAuthDataSource signUp()")
        return appResultOfSuspend {
            val signupResult = Firebase.auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = signupResult.user
            println("signupResult: $signupResult")
            println("firebaseUser: $firebaseUser")
            if (firebaseUser != null) {
                userInformation.copy(uid = firebaseUser.uid)
            } else {
                throw IllegalStateException("Authentication failed")
            }
        }
    }

    fun authStateListener(): Flow<Result<UserInformation, AppError>> = callbackFlow {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val currentUser = auth.currentUser
            if (currentUser != null) {
                trySend(Result.Success(UserInformation(uid = currentUser.uid)))
            } else {
                trySend(Result.Failure(AppErrors.authentication("Authentication failed")))
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
    ): Result<Unit, AppError> {
        return appResultOfSuspend {
            FirebaseAuth.getInstance().signOut()
            println("datasource logout result: ${FirebaseAuth.getInstance().currentUser}")
            if (familyId != null) {
                familyFirestoreDataSource.removeDeviceToken(uid, familyId)
            }
        }
    }
}
