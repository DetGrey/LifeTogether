package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation

interface UserRepository {
    suspend fun login(user: User): AuthResultListener
    suspend fun signUp(user: User, userInformation: UserInformation): AuthResultListener
    suspend fun getCurrentUser(): AuthResultListener
    suspend fun logout(): ResultListener
    suspend fun getUserInformation(uid: String): AuthResultListener
    suspend fun uploadUserInformation(userInformation: UserInformation): ResultListener
    suspend fun changeName(uid: String, newName: String): ResultListener
}
