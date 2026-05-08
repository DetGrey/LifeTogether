package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.Transient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class GroceryFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "GroceryFirestoreDS"
    }

    fun syncGroceryItems(familyId: String): Flow<Result<List<GroceryItem>, AppError>> = callbackFlow {
        val registration = db.collection(Constants.GROCERY_TABLE)
            .whereEqualTo("familyId", familyId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.Failure(AppErrors.fromThrowable(e)))
                    return@addSnapshotListener
                }
                snapshot?.let { qs ->
                    val items = mapFirestoreDocuments(
                        tag = TAG,
                        collectionName = Constants.GROCERY_TABLE,
                        entityName = "GroceryItem",
                        documents = qs.documents,
                    ) { doc ->
                        doc.toObject(GroceryItemDto::class.java)?.toDomain(doc.id)
                    }
                    trySend(Result.Success(items))
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveGroceryItem(groceryItem: GroceryItem): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.GROCERY_TABLE)
                .add(groceryItem.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun toggleGroceryItemCompletion(item: GroceryItem): Result<Unit, AppError> {
        return appResultOfSuspend {
            val id = item.id
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

    fun syncCategories(): Flow<Result<List<Category>, AppError>> = callbackFlow {
        val ref = db.collection(Constants.CATEGORY_TABLE)
        val registration = ref.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val categories = mapFirestoreDocuments(
                        tag = TAG,
                        collectionName = Constants.CATEGORY_TABLE,
                        entityName = "Category",
                        documents = snapshot.documents,
                    ) { doc ->
                        doc.toObject(CategoryDto::class.java)?.toDomain()
                    }
                    trySend(Result.Success(categories)).isSuccess
                }
        }
        awaitClose { registration.remove() }
    }

    suspend fun addCategory(category: Category): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.CATEGORY_TABLE)
                .add(category.toDto().toFirestoreMap()).await()
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

    fun syncGrocerySuggestions(): Flow<Result<List<GrocerySuggestion>, AppError>> = callbackFlow {
        val ref = db.collection(Constants.GROCERY_SUGGESTIONS_TABLE)
        val registration = ref.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val suggestions = mapFirestoreDocuments(
                        tag = TAG,
                        collectionName = Constants.GROCERY_SUGGESTIONS_TABLE,
                        entityName = "GrocerySuggestion",
                        documents = snapshot.documents,
                    ) { doc ->
                        doc.toObject(GrocerySuggestionDto::class.java)?.toDomain(doc.id)
                    }
                    trySend(Result.Success(suggestions)).isSuccess
                }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE)
                .add(suggestion.toDto().toFirestoreMap()).await()
        }
    }

    suspend fun updateGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(suggestion.id)
                .set(suggestion.toDto().toFirestoreMap()).await()
        }
    }

    suspend fun deleteGrocerySuggestion(suggestion: GrocerySuggestion): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(suggestion.id)
                .delete().await()
        }
    }
}

private data class CategoryDto(
    val emoji: String? = null,
    val name: String? = null,
) {
    fun toDomain(): Category? {
        val emojiValue = emoji?.takeIf { it.isNotBlank() } ?: return null
        val nameValue = name?.takeIf { it.isNotBlank() } ?: return null
        return Category(
            emoji = emojiValue,
            name = nameValue,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "emoji" to emoji,
        "name" to name,
    )
}

private data class GroceryItemDto(
    @DocumentId @Transient
    val id: String? = null,
    val familyId: String? = null,
    val itemName: String? = null,
    val lastUpdated: Date? = null,
    val completed: Boolean? = null,
    val category: CategoryDto? = null,
    val approxPrice: Float? = null,
) {
    fun toDomain(documentId: String): GroceryItem? {
        val familyIdValue = familyId?.takeIf { it.isNotBlank() } ?: return null
        val itemNameValue = itemName?.takeIf { it.isNotBlank() } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        val categoryValue = category?.toDomain() ?: return null
        return GroceryItem(
            id = documentId,
            familyId = familyIdValue,
            itemName = itemNameValue,
            lastUpdated = lastUpdatedValue,
            completed = completed ?: false,
            category = categoryValue,
            approxPrice = approxPrice,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "familyId" to familyId,
        "itemName" to itemName,
        "lastUpdated" to lastUpdated,
        "completed" to completed,
        "category" to category?.toFirestoreMap(),
        "approxPrice" to approxPrice,
    )
}

private data class GrocerySuggestionDto(
    @DocumentId @Transient
    val id: String? = null,
    val suggestionName: String? = null,
    val category: CategoryDto? = null,
    val approxPrice: Float? = null,
) {
    fun toDomain(documentId: String): GrocerySuggestion? {
        val suggestionNameValue = suggestionName?.takeIf { it.isNotBlank() } ?: return null
        val categoryValue = category?.toDomain() ?: return null
        return GrocerySuggestion(
            id = documentId,
            suggestionName = suggestionNameValue,
            category = categoryValue,
            approxPrice = approxPrice,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "suggestionName" to suggestionName,
        "category" to category?.toFirestoreMap(),
        "approxPrice" to approxPrice,
    )
}

private fun Category.toDto(): CategoryDto = CategoryDto(
    emoji = emoji,
    name = name,
)

private fun GroceryItem.toDto(): GroceryItemDto = GroceryItemDto(
    id = id,
    familyId = familyId,
    itemName = itemName,
    lastUpdated = lastUpdated,
    completed = completed,
    category = category.toDto(),
    approxPrice = approxPrice,
)

private fun GrocerySuggestion.toDto(): GrocerySuggestionDto = GrocerySuggestionDto(
    id = id,
    suggestionName = suggestionName,
    category = category.toDto(),
    approxPrice = approxPrice,
)
