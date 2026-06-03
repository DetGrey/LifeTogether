package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.TravellerPinsDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.TravellerPinEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TravellerPinsLocalSource @Inject constructor(
    private val travellerPinsDao: TravellerPinsDao,
) {
    fun observePins(familyId: String): Flow<List<TravellerPinEntity>> =
        travellerPinsDao.getItems(familyId)

    suspend fun getPinOnce(id: String): TravellerPinEntity? =
        travellerPinsDao.getItemOnce(id)

    suspend fun upsertPin(entity: TravellerPinEntity) =
        travellerPinsDao.updateItems(listOf(entity))

    suspend fun deletePin(id: String) =
        travellerPinsDao.deleteItems(listOf(id))

    suspend fun syncPins(entities: List<TravellerPinEntity>, familyId: String) {
        val current = travellerPinsDao.getItems(familyId).first()
        val toUpdate = computeItemsToUpdate(current, entities) { it.id }
        val toDelete = computeItemsToDelete(current, entities) { it.id }
        travellerPinsDao.updateItems(toUpdate)
        travellerPinsDao.deleteItems(toDelete.map { it.id })
    }

    suspend fun deleteFamilyPins(familyId: String) {
        travellerPinsDao.getItems(familyId).firstOrNull()?.let { pins ->
            travellerPinsDao.deleteItems(pins.map { it.id })
        }
    }
}
