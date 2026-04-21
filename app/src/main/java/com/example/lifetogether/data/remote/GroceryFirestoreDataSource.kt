package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrorThrowable
import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

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

    fun grocerySnapshotListener(familyId: String): Flow<Result<List<GroceryItem>, AppError>> = callbackFlow {
        val registration = db.collection(Constants.GROCERY_TABLE)
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.Failure(AppErrors.fromThrowable(e)))
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

    suspend fun saveGroceryItem(item: Item): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.GROCERY_TABLE).add(item).await()
            doc.id
        }
    }

    suspend fun toggleGroceryItemCompletion(item: CompletableItem): Result<Unit, AppError> {
        return appResultOfSuspend {
            val id = item.id ?: throw AppErrorThrowable(AppErrors.validation("Missing item id"))
            db.collection(Constants.GROCERY_TABLE).document(id).update(
                mapOf(
                    "completed" to item.completed,
                    "lastUpdated" to Date(System.currentTimeMillis()),
                ),
            ).await()
        }
    }

    suspend fun deleteGroceryItems(itemIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            itemIds.forEach { id -> batch.delete(db.collection(Constants.GROCERY_TABLE).document(id)) }
            batch.commit().await()
        }
    }

    fun categoriesSnapshotListener() = callbackFlow {
        val ref = db.collection(Constants.CATEGORY_TABLE)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val categories = snapshot.documents.mapNotNull { it.toObject(Category::class.java) }
                trySend(Result.Success(categories)).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun addCategory(category: Category): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.CATEGORY_TABLE).add(category).await()
        }
    }

    suspend fun deleteCategory(category: Category): Result<Unit, AppError> {
        return appResultOfSuspend {
            val query =
                db.collection(Constants.CATEGORY_TABLE)
                    .whereEqualTo("name", category.name)
                    .get()
                    .await()
            if (query.documents.isNotEmpty()) query.documents[0].reference.delete().await()
        }
    }

    fun grocerySuggestionsSnapshotListener() = callbackFlow {
        val ref = db.collection(Constants.GROCERY_SUGGESTIONS_TABLE)
        val registration = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
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

    suspend fun addGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).add(suggestion).await()
        }
    }

    suspend fun updateGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, AppError> {
        val id = suggestion.id ?: return Result.Failure(AppErrors.validation("Missing grocery suggestion id"))
        return appResultOfSuspend {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(id).set(suggestion).await()
        }
    }

    suspend fun deleteGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, AppError> {
        val id = suggestion.id ?: return Result.Failure(AppErrors.validation("Problems with grocery suggestion id"))
        return appResultOfSuspend {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(id).delete().await()
        }
    }
}
