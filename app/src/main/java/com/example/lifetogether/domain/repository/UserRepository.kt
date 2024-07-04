package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.ResultListener

interface UserRepository {
//    suspend fun login(user: User): AuthResultListener
//    suspend fun signUp(user: User, userInformation: UserInformation): AuthResultListener
    fun logout(): ResultListener
//    suspend fun changeName(uid: String, newName: String): ResultListener
}
