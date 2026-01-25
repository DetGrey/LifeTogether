package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.repository.StorageRepository
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import javax.inject.Inject

class ObserveRecipesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val storageRepository: StorageRepository,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
    ) {
        println("ObserveRecipesUseCase invoked")
        firestoreDataSource.recipeSnapshotListener(familyId).collect { result ->
            println("recipeSnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    if (result.listItems.isEmpty()) {
                        println("recipeSnapshotListener().collect result: is empty")
                        localDataSource.deleteFamilyRecipes(familyId)
                    } else {
                        // println("recipeSnapshotListener().collect result: ${result.listItems.map { listOf(it.itemName, it.tags) }}")

                        // Get existing recipe IDs that already have images to avoid re-downloading
                        val existingRecipeIdsWithImages = localDataSource.getRecipeIdsWithImages(familyId)

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

                        localDataSource.updateRecipes(result.listItems, byteArrays)
                    }
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveRecipesUseCase failure: ${result.message}")
                }
            }
        }
    }
}
