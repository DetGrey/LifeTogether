package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.traveller.TravellerPin
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface TravellerRepository {
    fun observePins(familyId: String): Flow<Result<List<TravellerPin>, AppError>>
    fun syncPinsFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    suspend fun savePin(pin: TravellerPin): Result<Unit, AppError>
    suspend fun deletePin(familyId: String, pinId: String): Result<Unit, AppError>
}
