package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lifetogether.data.model.MealPlanEntity
import com.example.lifetogether.util.Constants.MEAL_PLAN_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM $MEAL_PLAN_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<MealPlanEntity>>

    @Query("SELECT * FROM $MEAL_PLAN_TABLE WHERE family_id = :familyId")
    suspend fun getItemsOnce(familyId: String): List<MealPlanEntity>

    @Query("SELECT * FROM $MEAL_PLAN_TABLE WHERE id = :mealPlanId")
    fun getItemById(mealPlanId: String): Flow<MealPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<MealPlanEntity>)

    @Query("DELETE FROM $MEAL_PLAN_TABLE WHERE id IN (:itemIds)")
    suspend fun deleteItems(itemIds: List<String>)

    @Query("DELETE FROM $MEAL_PLAN_TABLE WHERE family_id = :familyId")
    suspend fun deleteFamilyItems(familyId: String)
}
