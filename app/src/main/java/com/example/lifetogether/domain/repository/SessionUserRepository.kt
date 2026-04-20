package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.listener.ResultListener
import kotlinx.coroutines.flow.Flow

interface SessionUserRepository {
    fun removeSavedUserInformation(): ResultListener
    suspend fun logout(uid: String, familyId: String?): ResultListener
    suspend fun fetchUserInformation(uid: String): AuthResultListener
    fun observeUserInformation(uid: String): Flow<AuthResultListener>
}
