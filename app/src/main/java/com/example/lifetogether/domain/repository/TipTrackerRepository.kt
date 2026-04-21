package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface TipTrackerRepository {
    fun observeTips(familyId: String): Flow<Result<List<TipItem>, AppError>>
    fun syncTipsFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    suspend fun saveTip(tip: TipItem): Result<String, AppError>
    suspend fun deleteTip(tipId: String): Result<Unit, AppError>
}
