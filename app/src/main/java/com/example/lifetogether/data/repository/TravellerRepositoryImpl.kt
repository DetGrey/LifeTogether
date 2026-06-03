package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.TravellerPinsLocalSource
import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.model.TravellerPinEntity
import com.example.lifetogether.data.remote.TravellerFirestoreDataSource
import com.example.lifetogether.data.repository.internal.stampNow
import com.example.lifetogether.domain.model.traveller.PinType
import com.example.lifetogether.domain.model.traveller.TravellerPin
import com.example.lifetogether.domain.repository.TravellerRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TravellerRepositoryImpl @Inject constructor(
    private val localSource: TravellerPinsLocalSource,
    private val remoteSource: TravellerFirestoreDataSource,
) : TravellerRepository {

    override fun observePins(familyId: String): Flow<Result<List<TravellerPin>, AppError>> {
        return localSource.observePins(familyId).map { entities ->
            appResultOf { entities.map { it.toModel() } }
        }
    }

    override fun syncPinsFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return remoteSource.travellerPinsSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> appResultOfSuspend {
                    if (result.data.items.isEmpty()) {
                        localSource.deleteFamilyPins(familyId)
                    } else {
                        localSource.syncPins(result.data.items.map { it.toEntity() }, familyId)
                    }
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override suspend fun savePin(pin: TravellerPin): Result<Unit, AppError> {
        val stamped = pin.stampNow()
        localSource.upsertPin(stamped.toEntity())
        return when (val result = remoteSource.savePin(stamped)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                localSource.deletePin(stamped.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deletePin(familyId: String, pinId: String): Result<Unit, AppError> {
        val old = localSource.getPinOnce(pinId)
        localSource.deletePin(pinId)
        return when (val result = remoteSource.deletePin(familyId, pinId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (old != null) localSource.upsertPin(old)
                Result.Failure(result.error)
            }
        }
    }

    private fun TravellerPin.toEntity() = TravellerPinEntity(
        id = id,
        familyId = familyId,
        lastUpdated = lastUpdated,
        city = city,
        country = country,
        latitude = latitude,
        longitude = longitude,
        type = type.name,
        dateFrom = dateFrom,
        dateTo = dateTo,
        albumId = albumId,
    )

    private fun TravellerPinEntity.toModel() = TravellerPin(
        id = id,
        familyId = familyId,
        lastUpdated = lastUpdated,
        city = city,
        country = country,
        latitude = latitude,
        longitude = longitude,
        type = PinType.entries.firstOrNull { it.name == type } ?: PinType.VISITED,
        dateFrom = dateFrom,
        dateTo = dateTo,
        albumId = albumId,
    )
}
