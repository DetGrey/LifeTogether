package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
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
        userInformation: UserInformation,
    ): AuthResultListener {
        return try {
            val loginResult = Firebase.auth.createUserWithEmailAndPassword(user.email, user.password).await()
            val firebaseUser = loginResult.user
            if (firebaseUser != null) {
                val updatedUserInformation = userInformation.copy(uid = firebaseUser.uid)
                when (val uploadResult = uploadUserInformation(updatedUserInformation)) {
                    is ResultListener.Success -> AuthResultListener.Success(updatedUserInformation)
                    is ResultListener.Failure -> AuthResultListener.Failure(uploadResult.message)
                }
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
                return getUserInformation(firebaseUser.uid)
            } else {
                AuthResultListener.Failure("Authentication failed")
            }
        } catch (e: Exception) {
            AuthResultListener.Failure("Error: ${e.message}")
        }
    }
    override suspend fun logout(): ResultListener {
        return try {
            Firebase.auth.signOut()
            ResultListener.Success
        } catch (e: Exception) {
            ResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun getUserInformation(uid: String): AuthResultListener {
        val db = Firebase.firestore
        return try {
            val documentSnapshot = db.collection("users").document(uid).get().await()
            println("documentSnapshot: $documentSnapshot")
            val userInformation = documentSnapshot.toObject(UserInformation::class.java)
            println("userInformation: $userInformation")

            if (userInformation != null) {
                AuthResultListener.Success(
                    userInformation,
                )
            } else {
                AuthResultListener.Failure("Could not fetch document")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun uploadUserInformation(userInformation: UserInformation): ResultListener {
        val db = Firebase.firestore
        return try {
            if (userInformation.uid != null) {
                db.collection("users").document(userInformation.uid).set(userInformation)

                return ResultListener.Success
            } else {
                return ResultListener.Failure("Cannot upload without being logged in")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            ResultListener.Failure("Error: ${e.message}")
        }
    }
}
