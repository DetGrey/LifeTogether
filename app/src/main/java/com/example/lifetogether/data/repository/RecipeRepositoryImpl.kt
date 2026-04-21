package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.remote.RecipeFirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecipeRepositoryImpl @Inject constructor(
    private val recipeLocalDataSource: RecipeLocalDataSource,
    private val recipeFirestoreDataSource: RecipeFirestoreDataSource,
    private val storageDataSource: StorageDataSource,
) : RecipeRepository {

    override fun observeRecipes(familyId: String): Flow<Result<List<Recipe>, AppError>> {
        return recipeLocalDataSource.observeRecipes(familyId)
            .map { entities ->
                try {
                    Result.Success(entities.map { it.toModel() }.sortedBy { it.itemName })
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override fun syncRecipesFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return recipeFirestoreDataSource.recipeSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> runCatching {
                    if (result.data.items.isEmpty()) {
                        recipeLocalDataSource.deleteFamilyRecipes(familyId)
                    } else {
                        val existingRecipeIdsWithImages = recipeLocalDataSource.getRecipeIdsWithImages(familyId)
                        val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                        for (recipe in result.data.items) {
                            if (recipe.id != null && existingRecipeIdsWithImages.contains(recipe.id)) {
                                continue
                            }
                            val byteArrayResult = recipe.imageUrl?.let { url ->
                                storageDataSource.fetchImageByteArray(url)
                            }
                            if (byteArrayResult is Result.Success) {
                                recipe.id?.let { byteArrays[it] = byteArrayResult.data }
                            }
                        }
                        recipeLocalDataSource.updateRecipes(result.data.items, byteArrays)
                    }
                    Result.Success(Unit)
                }.getOrElse { error ->
                    Result.Failure(error.message ?: "Failed to sync recipes")
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override fun observeRecipeById(familyId: String, id: String): Flow<Result<Recipe, AppError>> {
        return recipeLocalDataSource.observeRecipeById(familyId, id)
            .map { entity ->
                try {
                    if (entity != null) {
                        Result.Success(entity.toModel())
                    } else {
                        Result.Failure("Recipe not found")
                    }
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override suspend fun saveRecipe(recipe: Recipe): Result<String, AppError> {
        return recipeFirestoreDataSource.saveRecipe(recipe)
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit, AppError> {
        return recipeFirestoreDataSource.updateRecipe(recipe)
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit, AppError> {
        return when (val result = recipeFirestoreDataSource.deleteRecipe(recipeId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> Result.Failure(result.error)
        }
    }

    private fun RecipeEntity.toModel() = Recipe(
        id = id,
        familyId = familyId,
        itemName = itemName,
        lastUpdated = lastUpdated,
        description = description,
        ingredients = ingredients,
        instructions = instructions,
        preparationTimeMin = preparationTimeMin,
        favourite = favourite,
        servings = servings,
        tags = tags,
    )
}
