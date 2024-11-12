package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.RemoteAdminRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import javax.inject.Inject

class DeleteCategoryUseCase @Inject constructor(
    private val remoteAdminRepositoryImpl: RemoteAdminRepositoryImpl,
    private val localListRepositoryImpl: LocalListRepositoryImpl,
) {
    suspend operator fun invoke(
        category: Category,
    ): ResultListener {
        println("Inside DeleteCategoryUseCase and trying to delete a category")
        val firestoreResult = remoteAdminRepositoryImpl.deleteCategory(category)
        return firestoreResult
//        val roomResult = localListRepositoryImpl.deleteItems(listName, items.map { it.id.toString() })
//        return if (firestoreResult == ResultListener.Success) {
//            roomResult
//        } else {
//            firestoreResult
//        }
    }
}