package com.example.lifetogether.domain.usecase.observers

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.remote.FirebaseStorageDataSource
import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ByteArrayResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import javax.inject.Inject

class ObserveGalleryImagesUseCase @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
    private val firebaseStorageDataSource: FirebaseStorageDataSource,
    private val localDataSource: LocalDataSource,
) {
    suspend operator fun invoke(
        familyId: String,
    ) {
        println("ObserveGalleryImagesUseCase invoked")
        firestoreDataSource.galleryImagesSnapshotListener(familyId).collect { result ->
            println("galleryImagesSnapshotListener().collect result: $result")
            when (result) {
                is ListItemsResultListener.Success -> {
                    if (result.listItems.isEmpty()) {
                        println("galleryImagesSnapshotListener().collect result: is empty")
                        localDataSource.deleteFamilyGalleryImages(familyId)
                    } else {
                        val byteArrays: MutableMap<String, ByteArray> = mutableMapOf()
                        for (recipe in result.listItems) {
                            val byteArrayResult: ByteArrayResultListener? =
                                recipe.imageUrl?.let { url ->
                                    firebaseStorageDataSource.downloadImage(url)
                                }

                            if (byteArrayResult is ByteArrayResultListener.Success) {
                                recipe.id?.let { byteArrays.put(it, byteArrayResult.byteArray) }
                            }
                        }

                        localDataSource.updateGalleryImages(result.listItems, byteArrays)
                    }
                }
                is ListItemsResultListener.Failure -> {
                    // Handle failure
                    println("ObserveGalleryImagesUseCase failure: ${result.message}")
                }
            }
        }
    }
}
