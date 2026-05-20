package com.example.lifetogether.data.remote

import com.example.lifetogether.data.logic.AppErrors
import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.example.lifetogether.domain.result.ListSnapshot
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.DocumentId
import kotlin.jvm.Transient
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
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
                val migrationCandidates = mutableListOf<Pair<String, Recipe>>()
                val items = mapFirestoreDocuments(
                    tag = TAG,
                    collectionName = Constants.RECIPES_TABLE,
                    entityName = "Recipe",
                    documents = snapshot.documents,
                ) { doc ->
                    doc.toObject(RecipeDto::class.java)?.toDomainResult(doc.id)?.also { result ->
                        if (result.needsChildIdBackfill) {
                            migrationCandidates += doc.id to result.recipe
                        }
                    }?.recipe
                }
                migrationCandidates.forEach { (documentId, recipe) ->
                    launch {
                        db.collection(Constants.RECIPES_TABLE)
                            .document(documentId)
                            .set(recipe.toDto().toFirestoreMap(), SetOptions.merge())
                            .await()
                    }
                }
                trySend(Result.Success(ListSnapshot(items))).isSuccess
            } else {
                trySend(Result.Failure(AppErrors.storage("Empty snapshot"))).isSuccess
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun saveRecipe(recipe: Recipe): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.RECIPES_TABLE)
                .document(recipe.id)
                .set(recipe.toDto().toFirestoreMap())
                .await()
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit, AppError> {
        val id = recipe.id
        return appResultOfSuspend {
            db.collection(Constants.RECIPES_TABLE).document(id).set(recipe.toDto().toFirestoreMap(), SetOptions.merge()).await()
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.RECIPES_TABLE).document(recipeId).delete().await()
        }
    }

    suspend fun getRecipeImageUrl(recipeId: String): Result<String, AppError> {
        val docResult = try {
            val doc = db.collection(Constants.RECIPES_TABLE).document(recipeId).get().await()
            Result.Success(doc)
        } catch (throwable: Throwable) {
            Result.Failure(AppErrors.fromThrowable(throwable))
        }
        return when (docResult) {
            is Result.Success -> {
                val url = docResult.data.getString("imageUrl")
                if (url != null) Result.Success(url) else Result.Failure(AppErrors.notFound("Recipe image not found"))
            }

            is Result.Failure -> docResult
        }
    }

    suspend fun saveRecipeImageUrl(recipeId: String, url: String): Result<Unit, AppError> {
        return appResultOfSuspend {
            db.collection(Constants.RECIPES_TABLE).document(recipeId).update(mapOf("imageUrl" to url)).await()
        }
    }
}

private data class RecipeDto(
    @DocumentId @Transient
    val id: String? = null,
    val familyId: String? = null,
    val itemName: String? = null,
    val lastUpdated: Date? = null,
    val description: String? = null,
    val ingredients: List<IngredientDto>? = null,
    val instructions: List<InstructionDto>? = null,
    val preparationTimeMin: Int? = null,
    val favourite: Boolean? = null,
    val servings: Int? = null,
    val tags: List<String>? = null,
    val imageUrl: String? = null,
) {

    fun toDomainResult(documentId: String): RecipeDomainResult? {
        val familyIdValue = familyId?.takeIf { it.isNotBlank() } ?: return null
        val itemNameValue = itemName?.takeIf { it.isNotBlank() } ?: return null
        val lastUpdatedValue = lastUpdated ?: return null
        val descriptionValue = description ?: return null
        val ingredientsDtos = ingredients.orEmpty()
        val instructionsDtos = instructions.orEmpty()
        val ingredientsValue = ingredientsDtos.mapIndexedNotNull { index, dto -> dto.toDomain(index) }
        val instructionsValue = instructionsDtos.mapIndexedNotNull { index, dto -> dto.toDomain(index) }
        val preparationTimeMinValue = preparationTimeMin ?: return null
        val servingsValue = servings ?: return null
        val tagsValue = tags ?: return null
        return RecipeDomainResult(
            recipe = Recipe(
                id = documentId,
                familyId = familyIdValue,
                itemName = itemNameValue,
                lastUpdated = lastUpdatedValue,
                description = descriptionValue,
                ingredients = ingredientsValue,
                instructions = instructionsValue,
                preparationTimeMin = preparationTimeMinValue,
                favourite = favourite ?: false,
                servings = servingsValue,
                tags = tagsValue,
                imageUrl = imageUrl,
            ),
            needsChildIdBackfill = ingredientsDtos.any { it.needsBackfill() } ||
                instructionsDtos.any { it.needsBackfill() },
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "familyId" to familyId,
        "itemName" to itemName,
        "lastUpdated" to lastUpdated,
        "description" to description,
        "ingredients" to ingredients?.map { it.toFirestoreMap() },
        "instructions" to instructions?.map { it.toFirestoreMap() },
        "preparationTimeMin" to preparationTimeMin,
        "favourite" to favourite,
        "servings" to servings,
        "tags" to tags,
        "imageUrl" to imageUrl,
    )
}

private fun Recipe.toDto(): RecipeDto = RecipeDto(
    id = id,
    familyId = familyId,
    itemName = itemName,
    lastUpdated = lastUpdated,
    description = description,
    ingredients = ingredients.map { it.toDto() },
    instructions = instructions.map { it.toDto() },
    preparationTimeMin = preparationTimeMin,
    favourite = favourite,
    servings = servings,
    tags = tags,
    imageUrl = imageUrl,
)

private data class IngredientDto(
    val id: String? = null,
    val sortOrder: Int? = null,
    val amount: Double? = null,
    val measureType: String? = null,
    val itemName: String? = null,
    val completed: Boolean? = null,
) {
    fun toDomain(fallbackSortOrder: Int): Ingredient? {
        val idValue = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val amountValue = amount ?: return null
        val measureTypeValue = measureType?.toMeasureTypeOrNull() ?: return null
        val itemNameValue = itemName?.takeIf { it.isNotBlank() } ?: return null
        return Ingredient(
            id = idValue,
            amount = amountValue,
            measureType = measureTypeValue,
            itemName = itemNameValue,
            completed = completed ?: false,
            sortOrder = sortOrder ?: fallbackSortOrder,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "sortOrder" to sortOrder,
        "amount" to amount,
        "measureType" to measureType,
        "itemName" to itemName,
        "completed" to completed,
    ).filterValues { it != null }

    fun needsBackfill(): Boolean {
        return id.isNullOrBlank() || sortOrder == null
    }
}

private data class InstructionDto(
    val id: String? = null,
    val sortOrder: Int? = null,
    val itemName: String? = null,
    val completed: Boolean? = null,
) {
    fun toDomain(fallbackSortOrder: Int): Instruction? {
        val idValue = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        val itemNameValue = itemName?.takeIf { it.isNotBlank() } ?: return null
        return Instruction(
            id = idValue,
            itemName = itemNameValue,
            completed = completed ?: false,
            sortOrder = sortOrder ?: fallbackSortOrder,
        )
    }

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "sortOrder" to sortOrder,
        "itemName" to itemName,
        "completed" to completed,
    ).filterValues { it != null }

    fun needsBackfill(): Boolean {
        return id.isNullOrBlank() || sortOrder == null
    }
}

private fun Ingredient.toDto(): IngredientDto = IngredientDto(
    id = id,
    sortOrder = sortOrder,
    amount = amount,
    measureType = measureType.name,
    itemName = itemName,
    completed = completed,
)

private fun Instruction.toDto(): InstructionDto = InstructionDto(
    id = id,
    sortOrder = sortOrder,
    itemName = itemName,
    completed = completed,
)

private data class RecipeDomainResult(
    val recipe: Recipe,
    val needsChildIdBackfill: Boolean,
)

private fun String.toMeasureTypeOrNull(): MeasureType? {
    return MeasureType.entries.firstOrNull { measureType ->
        measureType.name.equals(this, ignoreCase = true) || measureType.unit.equals(this, ignoreCase = true)
    }
}
