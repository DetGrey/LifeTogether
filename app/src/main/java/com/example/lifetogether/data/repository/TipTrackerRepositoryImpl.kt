package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.source.TipTrackerLocalDataSource
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.data.remote.TipTrackerFirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.repository.TipTrackerRepository
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TipTrackerRepositoryImpl @Inject constructor(
    private val tipTrackerLocalDataSource: TipTrackerLocalDataSource,
    private val tipTrackerFirestoreDataSource: TipTrackerFirestoreDataSource,
) : TipTrackerRepository {

    override fun observeTips(familyId: String): Flow<Result<List<TipItem>, AppError>> {
        return tipTrackerLocalDataSource.observeTips(familyId).map { entities ->
            try {
                Result.Success(entities.map { it.toModel() })
            } catch (e: Exception) {
                Result.Failure(AppErrors.fromThrowable(e))
            }
        }
    }

    override fun syncTipsFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return tipTrackerFirestoreDataSource.tipTrackerSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> runCatching {
                    if (result.data.items.isEmpty()) {
                        tipTrackerLocalDataSource.deleteFamilyTipItems(familyId)
                    } else {
                        tipTrackerLocalDataSource.updateTipTracker(result.data.items)
                    }
                    Result.Success(Unit)
                }.getOrElse { error ->
                    Result.Failure(AppErrors.fromThrowable(error))
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override suspend fun saveTip(tip: TipItem): Result<String, AppError> {
        return tipTrackerFirestoreDataSource.saveTip(tip)
    }

    override suspend fun deleteTip(tipId: String): Result<Unit, AppError> {
        return when (val result = tipTrackerFirestoreDataSource.deleteTip(tipId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    private fun TipEntity.toModel() = TipItem(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        amount = amount,
        currency = currency,
        date = date,
    )
}
