package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface TipTrackerRepository {
    fun observeTips(familyId: String): Flow<Result<List<TipItem>, String>>
    suspend fun saveTip(tip: TipItem): Result<String, String>
    suspend fun deleteTip(tipId: String): Result<Unit, String>
}
