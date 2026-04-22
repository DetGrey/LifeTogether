package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.domain.model.recipe.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeLocalDataSource @Inject constructor(
    private val recipesDao: RecipesDao,
) {
    fun observeRecipes(familyId: String): Flow<List<RecipeEntity>> = recipesDao.getItems(familyId)

    fun observeRecipeById(familyId: String, id: String): Flow<RecipeEntity?> = recipesDao.getItemByIdFlow(familyId, id)

    suspend fun updateRecipes(
        items: List<Recipe>,
        byteArrays: Map<String, ByteArray>,
    ) {
        val familyId = items.firstOrNull()?.familyId ?: return
        var entities = items.map { item ->
            RecipeEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                description = item.description,
                ingredients = item.ingredients,
                instructions = item.instructions,
                preparationTimeMin = item.preparationTimeMin,
                favourite = item.favourite,
                servings = item.servings,
                tags = item.tags,
            )
        }
        if (byteArrays.isNotEmpty()) {
            entities = entities.map { item ->
                item.copy(imageData = byteArrays[item.id])
            }
        }

        val currentItems = recipesDao.getItems(familyId).first()
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
        recipesDao.updateItems(itemsToUpdate)
        recipesDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyRecipes(familyId: String) {
        recipesDao.getItems(familyId).firstOrNull()?.let { currentFamilyItems ->
            recipesDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    suspend fun getRecipeIdsWithImages(familyId: String): Set<String> = recipesDao.getRecipeIdsWithImages(familyId).toSet()

    fun observeImageByteArray(
        familyId: String,
        recipeId: String,
    ) = recipesDao.observeImageByteArray(familyId, recipeId)
}
