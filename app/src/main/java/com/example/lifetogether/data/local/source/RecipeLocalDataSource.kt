package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.RecipeIngredientEntity
import com.example.lifetogether.data.model.RecipeInstructionEntity
import com.example.lifetogether.data.model.RecipeWithChildren
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
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
    fun observeRecipes(familyId: String): Flow<List<RecipeWithChildren>> = recipesDao.getItems(familyId)

    fun observeRecipeById(familyId: String, id: String): Flow<RecipeWithChildren?> = recipesDao.getItemByIdFlow(familyId, id)

    suspend fun updateRecipes(
        items: List<Recipe>,
        byteArrays: Map<String, ByteArray>,
    ) {
        val familyId = items.firstOrNull()?.familyId ?: return
        val currentItems = recipesDao.getItemsOnce(familyId)
        val currentItemsById = currentItems.associateBy { it.recipe.id }
        val incomingIds = items.map { it.id }.toSet()

        val recipeEntities = items.map { item ->
            val existingItem = currentItemsById[item.id]
            val newImage = byteArrays[item.id]
            val imageData = when {
                newImage != null -> newImage
                existingItem?.recipe?.imageUrl == item.imageUrl -> existingItem?.recipe?.imageData
                else -> null
            }
            item.toRecipeEntity(imageData = imageData, imageUrl = item.imageUrl)
        }
        val ingredientEntities = items.flatMap { item ->
            item.ingredients.map { ingredient ->
                ingredient.toEntity(recipeId = item.id, sortOrder = ingredient.sortOrder)
            }
        }
        val instructionEntities = items.flatMap { item ->
            item.instructions.map { instruction ->
                instruction.toEntity(recipeId = item.id, sortOrder = instruction.sortOrder)
            }
        }

        recipesDao.updateItems(recipeEntities)
        recipesDao.deleteIngredientsByRecipeIds(incomingIds.toList())
        recipesDao.deleteInstructionsByRecipeIds(incomingIds.toList())
        recipesDao.updateRecipeIngredients(ingredientEntities)
        recipesDao.updateRecipeInstructions(instructionEntities)

        val itemsToDelete = currentItems.filterNot { it.recipe.id in incomingIds }
        if (itemsToDelete.isNotEmpty()) {
            recipesDao.deleteItems(itemsToDelete.map { it.recipe.id })
        }
    }

    suspend fun getRecipeOnce(id: String): RecipeWithChildren? = recipesDao.getItemOnce(id)

    suspend fun getRecipesOnce(familyId: String): List<RecipeWithChildren> = recipesDao.getItems(familyId).first()

    suspend fun upsertRecipe(
        recipe: Recipe,
        imageData: ByteArray? = null,
        imageUrl: String? = null,
    ) {
        val currentItem = recipesDao.getItemOnce(recipe.id)
        val resolvedImageData = imageData ?: currentItem?.recipe?.imageData
        val resolvedImageUrl = imageUrl ?: currentItem?.recipe?.imageUrl ?: recipe.imageUrl
        recipesDao.updateItems(listOf(recipe.toRecipeEntity(imageData = resolvedImageData, imageUrl = resolvedImageUrl)))
        recipesDao.deleteIngredientsByRecipeIds(listOf(recipe.id))
        recipesDao.deleteInstructionsByRecipeIds(listOf(recipe.id))
        recipesDao.updateRecipeIngredients(recipe.ingredients.map { ingredient ->
            ingredient.toEntity(recipeId = recipe.id, sortOrder = ingredient.sortOrder)
        })
        recipesDao.updateRecipeInstructions(recipe.instructions.map { instruction ->
            instruction.toEntity(recipeId = recipe.id, sortOrder = instruction.sortOrder)
        })
    }

    suspend fun deleteRecipe(id: String) = recipesDao.deleteItems(listOf(id))

    suspend fun deleteFamilyRecipes(familyId: String) {
        recipesDao.getItems(familyId).firstOrNull()?.let { currentFamilyItems ->
            recipesDao.deleteItems(currentFamilyItems.map { it.recipe.id })
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

    private fun Recipe.toRecipeEntity(
        imageData: ByteArray? = null,
        imageUrl: String? = null,
    ) = RecipeEntity(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        description = description,
        preparationTimeMin = preparationTimeMin,
        favourite = favourite,
        servings = servings,
        tags = tags,
        imageData = imageData,
        imageUrl = imageUrl ?: this.imageUrl,
    )

    private fun Ingredient.toEntity(
        recipeId: String,
        sortOrder: Int,
    ) = RecipeIngredientEntity(
        id = id,
        recipeId = recipeId,
        sortOrder = sortOrder,
        amount = amount,
        measureType = measureType.name,
        itemName = itemName,
        completed = completed,
    )

    private fun Instruction.toEntity(
        recipeId: String,
        sortOrder: Int,
    ) = RecipeInstructionEntity(
        id = id,
        recipeId = recipeId,
        sortOrder = sortOrder,
        itemName = itemName,
        completed = completed,
    )
}
