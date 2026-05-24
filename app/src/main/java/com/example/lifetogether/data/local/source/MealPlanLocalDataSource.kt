package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.MealPlanDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.MealPlanEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealPlanLocalDataSource @Inject constructor(
    private val mealPlanDao: MealPlanDao,
) {
    fun observeMealPlansByFamilyId(familyId: String): Flow<List<MealPlanEntity>> {
        return mealPlanDao.getItems(familyId)
    }

    fun observeMealPlan(mealPlanId: String): Flow<MealPlanEntity?> {
        return mealPlanDao.getItemById(mealPlanId)
    }

    suspend fun updateMealPlans(entities: List<MealPlanEntity>) {
        if (entities.isEmpty()) return
        val currentItems = mealPlanDao.getItemsOnce(entities.first().familyId)
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        val itemsToDelete = computeItemsToDelete(
            currentItems = currentItems,
            incomingItems = entities,
            key = { it.id },
        )
        mealPlanDao.updateItems(itemsToUpdate)
        if (itemsToDelete.isNotEmpty()) {
            mealPlanDao.deleteItems(itemsToDelete.map { it.id })
        }
    }

    suspend fun getMealPlanOnce(id: String): MealPlanEntity? = mealPlanDao.getItemOnce(id)

    suspend fun upsertMealPlan(entity: MealPlanEntity) = mealPlanDao.updateItems(listOf(entity))

    suspend fun deleteMealPlan(id: String) = mealPlanDao.deleteItems(listOf(id))

    suspend fun deleteFamilyMealPlans(familyId: String) {
        mealPlanDao.deleteFamilyItems(familyId)
    }
}
