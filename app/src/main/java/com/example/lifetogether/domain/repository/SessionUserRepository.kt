package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.UserInformation
import kotlinx.coroutines.flow.Flow

interface SessionUserRepository {
    suspend fun deleteSavedUserInformation(): Result<Unit, AppError>
    suspend fun logout(uid: String, familyId: String?): Result<Unit, AppError>
    suspend fun fetchUserInformation(uid: String): Result<UserInformation, AppError>
    fun observeUserInformation(uid: String): Flow<Result<UserInformation, AppError>>
}
