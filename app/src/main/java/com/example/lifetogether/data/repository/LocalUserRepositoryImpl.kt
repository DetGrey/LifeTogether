package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.User
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.repository.UserRepository

class LocalUserRepositoryImpl : UserRepository {
    override suspend fun login(
        user: User,
    ): AuthResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun signUp(
        user: User,
        userInformation: UserInformation,
    ): AuthResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentUser(): AuthResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): ResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun changeName(uid: String, newName: String): ResultListener {
        TODO("Not yet implemented")
    }
}
