package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteAdminRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val remoteAdminRepositoryImpl: RemoteAdminRepositoryImpl,
) {
    suspend operator fun invoke(
        category: Category,
    ): ResultListener {
        println("Inside DeleteCategoryUseCase and trying to delete a category")
        val firestoreResult = remoteAdminRepositoryImpl.deleteCategory(category)
        return firestoreResult
    }
}
