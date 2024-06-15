package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User

interface UserRepository {
    suspend fun login(user: User): AuthResultListener
    suspend fun signUp(user: User): AuthResultListener
    suspend fun getCurrentUser(): AuthResultListener
    suspend fun logout(): ResultListener
    suspend fun getUserInformation(uid: String): AuthResultListener
}
