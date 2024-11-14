package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.lifetogether.data.model.RecipeEntity

@Dao
interface RecipesDao {
    @Query("SELECT * FROM recipes")
    fun getAll(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getRecipeById(familyId: String, id: String): RecipeEntity?

    @Insert
    fun insert(recipe: RecipeEntity)
}
