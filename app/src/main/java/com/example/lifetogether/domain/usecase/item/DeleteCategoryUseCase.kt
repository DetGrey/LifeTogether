package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.AdminRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.Category
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val adminRepositoryImpl: AdminRepositoryImpl,
) {
    suspend operator fun invoke(
        category: Category,
    ): ResultListener {
        println("Inside DeleteCategoryUseCase and trying to delete a category")
        val firestoreResult = adminRepositoryImpl.deleteCategory(category)
        return ResultListener.Failure("removed")
    }
}
