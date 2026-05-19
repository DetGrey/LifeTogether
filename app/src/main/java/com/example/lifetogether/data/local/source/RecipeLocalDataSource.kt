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
        val currentItems = recipesDao.getItems(familyId).first()
        val currentItemsById = currentItems.associateBy { it.id }
        val entities = items.map { item ->
            val existingItem = currentItemsById[item.id]
            val newImage = byteArrays[item.id]
            val imageData = when {
                newImage != null -> newImage
                existingItem?.imageUrl == item.imageUrl -> existingItem?.imageData
                else -> null
            }
            RecipeEntity(
                id = item.id,
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
                imageData = imageData,
                imageUrl = item.imageUrl,
            )
        }
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

    suspend fun getRecipeOnce(id: String): RecipeEntity? = recipesDao.getItemOnce(id)

    suspend fun getRecipesOnce(familyId: String): List<RecipeEntity> = recipesDao.getItems(familyId).first()

    suspend fun upsertRecipe(entity: RecipeEntity) = recipesDao.updateItems(listOf(entity))

    suspend fun deleteRecipe(id: String) = recipesDao.deleteItems(listOf(id))

    suspend fun deleteFamilyRecipes(familyId: String) {
        recipesDao.getItems(familyId).firstOrNull()?.let { currentFamilyItems ->
            recipesDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    fun observeImageByteArray(
        familyId: String,
        recipeId: String,
    ) = recipesDao.observeImageByteArray(familyId, recipeId)

    suspend fun updateRecipeImageByteArray(
        familyId: String,
        recipeId: String,
        imageData: ByteArray?,
    ) {
        recipesDao.updateImageByteArray(
            familyId = familyId,
            recipeId = recipeId,
            imageData = imageData,
        )
    }

    suspend fun updateRecipeImageUrl(
        familyId: String,
        recipeId: String,
        imageUrl: String?,
    ) {
        recipesDao.updateImageUrl(
            familyId = familyId,
            recipeId = recipeId,
            imageUrl = imageUrl,
        )
    }
}
