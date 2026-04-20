package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.TipTrackerLocalDataSource
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.repository.TipTrackerRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TipTrackerRepositoryImpl @Inject constructor(
    private val tipTrackerLocalDataSource: TipTrackerLocalDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) : TipTrackerRepository {

    override fun observeTips(familyId: String): Flow<Result<List<TipItem>, String>> {
        return tipTrackerLocalDataSource.observeTips(familyId).map { entities ->
            try {
                Result.Success(entities.map { it.toModel() })
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown mapping error")
            }
        }
    }

    override suspend fun saveTip(tip: TipItem): Result<String, String> {
        return firestoreDataSource.saveItem(tip, Constants.TIP_TRACKER_TABLE)
    }

    override suspend fun deleteTip(tipId: String): Result<Unit, String> {
        return when (val result = firestoreDataSource.deleteItem(tipId, Constants.TIP_TRACKER_TABLE)) {
            is ResultListener.Success -> Result.Success(Unit)
            is ResultListener.Failure -> Result.Failure(result.message)
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
