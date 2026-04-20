package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecipeRepositoryImpl @Inject constructor(
    private val recipeLocalDataSource: RecipeLocalDataSource,
    private val firestoreDataSource: FirestoreDataSource,
) : RecipeRepository {

    override fun observeRecipes(familyId: String): Flow<Result<List<Recipe>, String>> {
        return recipeLocalDataSource.observeRecipes(familyId)
            .map { entities ->
                try {
                    Result.Success(entities.map { it.toModel() }.sortedBy { it.itemName })
                } catch (e: Exception) {
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    override fun observeRecipeById(familyId: String, id: String): Flow<Result<Recipe, String>> {
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

    override suspend fun saveRecipe(recipe: Recipe): Result<String, String> {
        return firestoreDataSource.saveItem(recipe, Constants.RECIPES_TABLE)
    }

    override suspend fun updateRecipe(recipe: Recipe): Result<Unit, String> {
        return firestoreDataSource.updateItem(recipe, Constants.RECIPES_TABLE)
    }

    override suspend fun deleteRecipe(recipeId: String): Result<Unit, String> {
        return when (val result = firestoreDataSource.deleteItem(recipeId, Constants.RECIPES_TABLE)) {
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
