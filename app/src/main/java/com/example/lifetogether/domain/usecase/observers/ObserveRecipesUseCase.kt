package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.source.RecipeLocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveRecipesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageRepository: StorageRepository,
    private val recipeLocalDataSource: RecipeLocalDataSource,
) {
    fun start(
        scope: CoroutineScope,
        familyId: String,
    ): ObserverStartHandle {
        val firstSuccess = CompletableDeferred<Result<Unit>>()
        val job = scope.launch {
            println("ObserveRecipesUseCase invoked")
            firestoreDataSource.recipeSnapshotListener(familyId).collect { result ->
                println("recipeSnapshotListener().collect result: $result")
                when (result) {
                    is ListItemsResultListener.Success -> {
                        runCatching {
                            if (result.listItems.isEmpty()) {
                                println("recipeSnapshotListener().collect result: is empty")
                                recipeLocalDataSource.deleteFamilyRecipes(familyId)
                            } else {
                                // Get existing recipe IDs that already have images to avoid re-downloading
                                val existingRecipeIdsWithImages = recipeLocalDataSource.getRecipeIdsWithImages(familyId)

                                val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                                for (recipe in result.listItems) {
                                    // Skip download if this recipe already has an image stored locally
                                    if (recipe.id != null && existingRecipeIdsWithImages.contains(recipe.id)) {
                                        println("ObserveRecipesUseCase: Skipping download for ${recipe.itemName} - image already exists locally")
                                        continue
                                    }

                                    val byteArrayResult: ByteArrayResultListener? =
                                        recipe.imageUrl?.let { url ->
                                            storageRepository.fetchImageByteArray(url)
                                        }

                                    if (byteArrayResult is ByteArrayResultListener.Success) {
                                        recipe.id?.let { byteArrays.put(it, byteArrayResult.byteArray) }
                                    }
                                }

                                recipeLocalDataSource.updateRecipes(result.listItems, byteArrays)
                            }
                        }.onSuccess {
                            firstSuccess.completeFirstSuccessIfNeeded()
                        }.onFailure { error ->
                            println("ObserveRecipesUseCase local update failure: ${error.message}")
                        }
                    }
                    is ListItemsResultListener.Failure -> {
                        // Keep listener alive; firstSuccess is one-shot and only completes on success.
                        println("ObserveRecipesUseCase failure: ${result.message}")
                    }
                }
            }
        }
        return ObserverStartHandle(firstSuccess = firstSuccess, job = job)
    }
}
