package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.MealPlanDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.MealPlanEntity
import com.example.lifetogether.domain.model.mealplanner.MealPlan
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

    suspend fun updateMealPlans(items: List<MealPlan>) {
        if (items.isEmpty()) return
        val currentItems = mealPlanDao.getItemsOnce(items.first().familyId)
        val entities = items.map { it.toEntity() }
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

    suspend fun deleteFamilyMealPlans(familyId: String) {
        mealPlanDao.deleteFamilyItems(familyId)
    }

    private fun MealPlan.toEntity() = MealPlanEntity(
        id = id,
        familyId = familyId,
        itemName = itemName,
        date = date,
        recipeId = recipeId,
        customMealName = customMealName,
        mealType = mealType.name,
        notes = notes,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )
}
