package com.example.lifetogether.data.remote

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
                trySend(Result.Failure("Error: ${e.message}")).isSuccess
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
                trySend(Result.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveRecipe(recipe: Recipe): Result<String, String> {
        return try {
            val documentReference = db.collection(Constants.RECIPES_TABLE).add(recipe).await()
            Result.Success(documentReference.id)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit, String> {
        return try {
            val id = recipe.id ?: return Result.Failure("Missing recipe id")
            db.collection(Constants.RECIPES_TABLE).document(id).set(recipe, SetOptions.merge()).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit, String> {
        return try {
            db.collection(Constants.RECIPES_TABLE).document(recipeId).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun getRecipeImageUrl(recipeId: String): Result<String, String>? {
        return try {
            val doc = db.collection(Constants.RECIPES_TABLE).document(recipeId).get().await()
            doc.getString("imageUrl")?.let { Result.Success(it) }
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun saveRecipeImageUrl(recipeId: String, url: String): Result<Unit, String> {
        return try {
            db.collection(Constants.RECIPES_TABLE).document(recipeId).update(mapOf("imageUrl" to url)).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }
}
