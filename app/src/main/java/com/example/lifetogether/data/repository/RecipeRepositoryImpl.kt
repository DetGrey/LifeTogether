package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOf
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.data.remote.RecipeFirestoreDataSource
import com.example.lifetogether.domain.datasource.StorageDataSource
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
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
            .map { entities -> appResultOf { entities.map { it.toDomain() }.sortedBy { it.itemName } } }
    }

    override fun syncRecipesFromRemote(familyId: String): Flow<Result<Unit, AppError>> {
        return recipeFirestoreDataSource.recipeSnapshotListener(familyId).map { result ->
            when (result) {
                is Result.Success -> appResultOfSuspend {
                    if (result.data.items.isEmpty()) {
                        recipeLocalDataSource.deleteFamilyRecipes(familyId)
                    } else {
                        val currentRecipesById = recipeLocalDataSource.getRecipesOnce(familyId).associateBy { it.recipe.id }
                        val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                        for (recipe in result.data.items) {
                            val imageUrl = recipe.imageUrl
                            val currentRecipe = currentRecipesById[recipe.id]
                            val shouldDownloadImage = imageUrl != null && (
                                currentRecipe?.recipe?.imageUrl != imageUrl || currentRecipe.recipe.imageData == null
                            )
                            val byteArrayResult = if (shouldDownloadImage) {
                                storageDataSource.fetchImageByteArray(imageUrl)
                            } else {
                                null
                            }
                            if (byteArrayResult is Result.Success) {
                                byteArrays[recipe.id] = byteArrayResult.data
                            }
                        }
                        recipeLocalDataSource.updateRecipes(result.data.items, byteArrays)
                    }
                }

                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }

    override fun observeRecipeById(familyId: String, id: String): Flow<Result<Recipe, AppError>> {
        return recipeLocalDataSource.observeRecipeById(familyId, id)
            .map { entity ->
                appResultOf {
                    entity?.toDomain() ?: throw AppErrorThrowable(AppErrors.notFound("Recipe not found"))
                }
            }
    }

    override suspend fun saveRecipe(recipe: Recipe): Result<String, AppError> {
        recipeLocalDataSource.upsertRecipe(recipe)
        return when (val result = recipeFirestoreDataSource.saveRecipe(recipe)) {
            is Result.Success -> Result.Success(recipe.id)
            is Result.Failure -> {
                recipeLocalDataSource.deleteRecipe(recipe.id)
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit, AppError> {
        val oldEntity = recipeLocalDataSource.getRecipeOnce(recipe.id)
        recipeLocalDataSource.upsertRecipe(
            recipe = recipe,
            imageData = oldEntity?.recipe?.imageData,
            imageUrl = oldEntity?.recipe?.imageUrl,
        )
        return when (val result = recipeFirestoreDataSource.updateRecipe(recipe)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) {
                    recipeLocalDataSource.upsertRecipe(
                        recipe = oldEntity.toDomain(),
                        imageData = oldEntity.recipe.imageData,
                        imageUrl = oldEntity.recipe.imageUrl,
                    )
                } else {
                    recipeLocalDataSource.deleteRecipe(recipe.id)
                }
                Result.Failure(result.error)
            }
        }
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit, AppError> {
        val oldEntity = recipeLocalDataSource.getRecipeOnce(recipeId)
        recipeLocalDataSource.deleteRecipe(recipeId)
        return when (val result = recipeFirestoreDataSource.deleteRecipe(recipeId)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Failure -> {
                if (oldEntity != null) {
                    recipeLocalDataSource.upsertRecipe(
                        recipe = oldEntity.toDomain(),
                        imageData = oldEntity.recipe.imageData,
                        imageUrl = oldEntity.recipe.imageUrl,
                    )
                }
                Result.Failure(result.error)
            }
        }
    }
}
