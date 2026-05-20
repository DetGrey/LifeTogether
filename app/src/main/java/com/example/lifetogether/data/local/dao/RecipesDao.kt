package com.example.lifetogether.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.RecipeIngredientEntity
import com.example.lifetogether.data.model.RecipeInstructionEntity
import com.example.lifetogether.data.model.RecipeWithChildren
import com.example.lifetogether.util.Constants.RECIPES_TABLE
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipesDao {
    @Transaction
    @Query("SELECT * FROM $RECIPES_TABLE WHERE family_id = :familyId")
    fun getItems(familyId: String): Flow<List<RecipeWithChildren>>

    @Transaction
    @Query("SELECT * FROM $RECIPES_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun getItemByIdFlow(familyId: String, id: String): Flow<RecipeWithChildren?>

    @Transaction
    @Query("SELECT * FROM $RECIPES_TABLE WHERE id = :id LIMIT 1")
    suspend fun getItemOnce(id: String): RecipeWithChildren?

    @Transaction
    @Query("SELECT * FROM $RECIPES_TABLE WHERE family_id = :familyId")
    suspend fun getItemsOnce(familyId: String): List<RecipeWithChildren>

    @Query("SELECT image_data FROM $RECIPES_TABLE WHERE family_id = :familyId AND id = :id LIMIT 1")
    fun observeImageByteArray(familyId: String, id: String): Flow<ByteArray?>

    @Query("UPDATE $RECIPES_TABLE SET image_data = :imageData WHERE family_id = :familyId AND id = :recipeId")
    suspend fun updateImageByteArray(
        familyId: String,
        recipeId: String,
        imageData: ByteArray?,
    )

    @Query("UPDATE $RECIPES_TABLE SET image_url = :imageUrl WHERE family_id = :familyId AND id = :recipeId")
    suspend fun updateImageUrl(
        familyId: String,
        recipeId: String,
        imageUrl: String?,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateItems(items: List<RecipeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRecipeIngredients(items: List<RecipeIngredientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateRecipeInstructions(items: List<RecipeInstructionEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipe_id IN (:recipeIds)")
    suspend fun deleteIngredientsByRecipeIds(recipeIds: List<String>)

    @Query("DELETE FROM recipe_instructions WHERE recipe_id IN (:recipeIds)")
    suspend fun deleteInstructionsByRecipeIds(recipeIds: List<String>)

    @Query("DELETE FROM $RECIPES_TABLE WHERE id IN (:itemIds)")
    suspend fun deleteItems(itemIds: List<String>)
}
