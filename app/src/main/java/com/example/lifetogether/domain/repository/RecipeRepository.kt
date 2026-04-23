package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipes(familyId: String): Flow<Result<List<Recipe>, AppError>>
    fun syncRecipesFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    fun observeRecipeById(familyId: String, id: String): Flow<Result<Recipe, AppError>>
    suspend fun saveRecipe(recipe: Recipe): Result<String, AppError>
    suspend fun updateRecipe(recipe: Recipe): Result<Unit, AppError>
    suspend fun deleteRecipe(recipeId: String): Result<Unit, AppError>
}
