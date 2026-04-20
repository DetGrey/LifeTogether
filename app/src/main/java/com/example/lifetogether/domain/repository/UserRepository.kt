package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.Result

interface UserRepository {
    suspend fun changeName(uid: String, familyId: String?, newName: String): Result<Unit, String>
    suspend fun fetchFcmTokens(familyId: String): List<String>?
    suspend fun storeFcmToken(uid: String, familyId: String): Result<Unit, String>
}
