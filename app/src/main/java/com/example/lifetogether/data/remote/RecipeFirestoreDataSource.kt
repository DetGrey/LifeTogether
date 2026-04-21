package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import android.util.Log
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class RecipeFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "RecipeFirestoreDS"
    }
    fun recipeSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.RECIPES_TABLE).whereEqualTo("familyId", familyId)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.documents.mapNotNull { doc ->
                    runCatching { doc.toObject(Recipe::class.java)?.copy(id = doc.id) }
                        .onFailure { Log.e(TAG, "Failed parsing recipe ${doc.id}", it) }
                        .getOrNull()
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveRecipe(recipe: Recipe): Result<String, AppError> {
        return try {
            val documentReference = db.collection(Constants.RECIPES_TABLE).add(recipe).await()
            Result.Success(documentReference.id)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit, AppError> {
        return try {
            val id = recipe.id ?: return Result.Failure(AppErrors.validation("Missing recipe id"))
            db.collection(Constants.RECIPES_TABLE).document(id).set(recipe, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit, AppError> {
        return try {
            db.collection(Constants.RECIPES_TABLE).document(recipeId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun getRecipeImageUrl(recipeId: String): Result<String, AppError> {
        return try {
            val doc = db.collection(Constants.RECIPES_TABLE).document(recipeId).get().await()
            val url = doc.getString("imageUrl")
            if (url != null) Result.Success(url) else Result.Failure(AppErrors.notFound("Recipe image not found"))
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }

    suspend fun saveRecipeImageUrl(recipeId: String, url: String): Result<Unit, AppError> {
        return try {
            db.collection(Constants.RECIPES_TABLE).document(recipeId).update(mapOf("imageUrl" to url)).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(AppErrors.fromThrowable(e))
        }
    }
}
