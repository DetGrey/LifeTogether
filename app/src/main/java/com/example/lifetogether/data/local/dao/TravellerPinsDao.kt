package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.TravellerPinEntity
import com.example.lifetogether.util.Constants.TRAVELLER_PINS_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface TravellerPinsDao {
    @Query("SELECT * FROM $TRAVELLER_PINS_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<TravellerPinEntity>>

    @Query("SELECT * FROM $TRAVELLER_PINS_TABLE WHERE id = :id LIMIT 1")
    suspend fun getItemOnce(id: String): TravellerPinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<TravellerPinEntity>)

    @Query("DELETE FROM $TRAVELLER_PINS_TABLE WHERE id IN (:itemIds)")
    suspend fun deleteItems(itemIds: List<String>)

    @Query("DELETE FROM $TRAVELLER_PINS_TABLE WHERE family_id = :familyId")
    suspend fun deleteFamilyItems(familyId: String)
}
