package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.listener.ResultListener

interface UserRepository {
    suspend fun changeName(uid: String, familyId: String?, newName: String): ResultListener
    suspend fun fetchFcmTokens(familyId: String): List<String>?
    suspend fun storeFcmToken(uid: String, familyId: String): ResultListener
}
