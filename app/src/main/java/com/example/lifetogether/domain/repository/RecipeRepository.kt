package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipes(familyId: String): Flow<Result<List<Recipe>, String>>
    fun syncRecipesFromRemote(familyId: String): Flow<Result<Unit, String>>
    fun observeRecipeById(familyId: String, id: String): Flow<Result<Recipe, String>>
    suspend fun saveRecipe(recipe: Recipe): Result<String, String>
    suspend fun updateRecipe(recipe: Recipe): Result<Unit, String>
    suspend fun deleteRecipe(recipeId: String): Result<Unit, String>
}
