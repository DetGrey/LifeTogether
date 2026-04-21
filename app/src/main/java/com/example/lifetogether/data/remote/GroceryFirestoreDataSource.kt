package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class GroceryFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {

    fun grocerySnapshotListener(familyId: String): Flow<Result<List<GroceryItem>, String>> = callbackFlow {
        val registration = db.collection(Constants.GROCERY_TABLE)
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.Failure("Firestore Error: ${e.message}"))
                    return@addSnapshotListener
                }
                snapshot?.let { qs ->
                    val items = qs.documents.mapNotNull { doc ->
                        doc.toObject(GroceryItem::class.java)?.copy(id = doc.id)
                    }
                    trySend(Result.Success(items))
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveGroceryItem(item: Item): Result<String, String> {
        return try {
            val doc = db.collection(Constants.GROCERY_TABLE).add(item).await()
            Result.Success(doc.id)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun toggleGroceryItemCompletion(item: CompletableItem): Result<Unit, String> {
        return try {
            val id = item.id ?: return Result.Failure("Missing item id")
            db.collection(Constants.GROCERY_TABLE).document(id).update(
                mapOf(
                    "completed" to item.completed,
                    "lastUpdated" to Date(System.currentTimeMillis()),
                ),
            ).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, String> {
        return try {
            val batch = db.batch()
            itemIds.forEach { id -> batch.delete(db.collection(Constants.GROCERY_TABLE).document(id)) }
            batch.commit().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    fun categoriesSnapshotListener() = callbackFlow {
        val ref = db.collection(Constants.CATEGORY_TABLE)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val categories = snapshot.documents.mapNotNull { it.toObject(Category::class.java) }
                trySend(Result.Success(categories)).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun addCategory(category: Category): Result<Unit, String> {
        return try {
            db.collection(Constants.CATEGORY_TABLE).add(category).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteCategory(category: Category): Result<Unit, String> {
        return try {
            val query = db.collection(Constants.CATEGORY_TABLE).whereEqualTo("name", category.name).get().await()
            if (query.documents.isNotEmpty()) query.documents[0].reference.delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    fun grocerySuggestionsSnapshotListener() = callbackFlow {
        val ref = db.collection(Constants.GROCERY_SUGGESTIONS_TABLE)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val suggestions = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(GrocerySuggestion::class.java)?.copy(id = doc.id)
                }
                trySend(Result.Success(suggestions)).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun addGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, String> {
        return try {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).add(suggestion).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun updateGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, String> {
        val id = suggestion.id ?: return Result.Failure("Missing grocery suggestion id")
        return try {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(id).set(suggestion).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, String> {
        val id = suggestion.id ?: return Result.Failure("Problems with grocery suggestion id")
        return try {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(id).delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure("Error: ${e.message}")
        }
    }
}
