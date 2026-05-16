package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.model.lists.MealType
import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class MealPlanFirestoreDataSource @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private companion object {
        const val TAG = "MealPlanFirestoreDS"
    }

    fun familyMealPlansSnapshotListener(familyId: String) = callbackFlow {
        val ref = db.collection(Constants.MEAL_PLAN_TABLE)
            .whereEqualTo("familyId", familyId)
        val reg = ref.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Result.Failure(AppErrors.fromThrowable(e))).isSuccess
                return@addSnapshotListener
            }
            if (snapshot == null) {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
                return@addSnapshotListener
            }
            val items = mapFirestoreDocuments(
                tag = TAG,
                collectionName = Constants.MEAL_PLAN_TABLE,
                entityName = "MealPlan",
                documents = snapshot.documents,
            ) { doc ->
                doc.toObject(MealPlanDto::class.java)?.toDomain(doc.id)
            }
            trySend(Result.Success(ListSnapshot(items))).isSuccess
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveMealPlan(mealPlan: MealPlan): Result<String, AppError> {
        return appResultOfSuspend {
            val doc = db.collection(Constants.MEAL_PLAN_TABLE)
                .add(mealPlan.toDto().toFirestoreMap()).await()
            doc.id
        }
    }

    suspend fun updateMealPlan(mealPlan: MealPlan): Result<Unit, AppError> {
        val id = mealPlan.id
        if (id.isBlank()) return Result.Failure(AppErrors.validation("Missing meal plan id"))
        return appResultOfSuspend {
            db.collection(Constants.MEAL_PLAN_TABLE).document(id)
                .set(mealPlan.toDto().toFirestoreMap(), SetOptions.merge())
                .await()
        }
    }

    suspend fun deleteMealPlans(mealPlanIds: List<String>): Result<Unit, AppError> {
        return appResultOfSuspend {
            val batch = db.batch()
            mealPlanIds.forEach { id ->
                batch.delete(db.collection(Constants.MEAL_PLAN_TABLE).document(id))
            }
            batch.commit().await()
        }
    }

    private data class MealPlanDto(
        @DocumentId @Transient
        val id: String? = null,
        val familyId: String? = null,
        val name: String? = null,
        val itemName: String? = null,
        val date: String? = null,
        val recipeId: String? = null,
        val customMealName: String? = null,
        val mealType: String? = null,
        val notes: String? = null,
        val lastUpdated: Date? = null,
        val dateCreated: Date? = null,
    ) {
        fun toDomain(documentId: String): MealPlan? {
            val familyIdValue = familyId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val itemNameValue = name?.trim()?.takeIf { it.isNotEmpty() }
                ?: itemName?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val mealDateValue = date?.trim()?.takeIf { it.isNotEmpty() }
                ?: return null
            val lastUpdatedValue = lastUpdated ?: return null
            val dateCreatedValue = dateCreated ?: return null
            val mealTypeValue = mealType?.let { MealType.fromValue(it) } ?: MealType.DINNER

            return MealPlan(
                id = documentId,
                familyId = familyIdValue,
                itemName = itemNameValue,
                date = mealDateValue,
                recipeId = recipeId?.trim()?.takeIf { it.isNotEmpty() },
                customMealName = customMealName?.trim()?.takeIf { it.isNotEmpty() },
                mealType = mealTypeValue,
                notes = notes?.trim().orEmpty(),
                lastUpdated = lastUpdatedValue,
                dateCreated = dateCreatedValue,
            )
        }
    }

    private data class MealPlanWriteDto(
        val familyId: String?,
        val name: String?,
        val date: String?,
        val recipeId: String?,
        val customMealName: String?,
        val mealType: String?,
        val notes: String?,
        val lastUpdated: Date?,
        val dateCreated: Date?,
    ) {
        fun toFirestoreMap(): Map<String, Any?> = mapOf(
            "familyId" to familyId,
            "name" to name,
            "date" to date,
            "recipeId" to recipeId,
            "customMealName" to customMealName,
            "mealType" to mealType,
            "notes" to notes,
            "lastUpdated" to lastUpdated,
            "dateCreated" to dateCreated,
        )
    }

    private fun MealPlan.toDto(): MealPlanWriteDto = MealPlanWriteDto(
        familyId = familyId,
        name = itemName,
        date = date,
        recipeId = recipeId,
        customMealName = customMealName,
        mealType = mealType.name,
        notes = notes,
        lastUpdated = lastUpdated,
        dateCreated = dateCreated,
    )
}
