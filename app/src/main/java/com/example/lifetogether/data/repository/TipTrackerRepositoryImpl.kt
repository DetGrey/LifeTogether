package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.source.TipTrackerLocalDataSource
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.data.remote.TipTrackerFirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.repository.TipTrackerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TipTrackerRepositoryImpl @Inject constructor(
    private val tipTrackerLocalDataSource: TipTrackerLocalDataSource,
    private val tipTrackerFirestoreDataSource: TipTrackerFirestoreDataSource,
) : TipTrackerRepository {

    override fun observeTips(familyId: String): Flow<Result<List<TipItem>, AppError>> {
        return tipTrackerLocalDataSource.observeTips(familyId).map { entities ->
            appResultOf { entities.map { it.toModel() } }
        }
    }

    override fun syncTipsFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return tipTrackerFirestoreDataSource.tipTrackerSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> appResultOfSuspend {
                    if (result.data.items.isEmpty()) {
                        tipTrackerLocalDataSource.deleteFamilyTipItems(familyId)
                    } else {
                        tipTrackerLocalDataSource.updateTipTracker(result.data.items)
                    }
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override suspend fun saveTip(tip: TipItem): Result<String, AppError> {
        tipTrackerLocalDataSource.upsertTip(tip.toEntity())
        return when (val result = tipTrackerFirestoreDataSource.saveTip(tip)) {
            is Result.Success -> Result.Success(tip.id)
            is Result.Failure -> {
                tipTrackerLocalDataSource.deleteTip(tip.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteTip(tipId: String): Result<Unit, AppError> {
        val oldEntity = tipTrackerLocalDataSource.getTipOnce(tipId)
        tipTrackerLocalDataSource.deleteTip(tipId)
        return when (val result = tipTrackerFirestoreDataSource.deleteTip(tipId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) tipTrackerLocalDataSource.upsertTip(oldEntity)
                Result.Failure(result.error)
            }
        }
    }

    private fun TipItem.toEntity() = TipEntity(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        amount = amount,
        currency = currency,
        date = date,
    )

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
