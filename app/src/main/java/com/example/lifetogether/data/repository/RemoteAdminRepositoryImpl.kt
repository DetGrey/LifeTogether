package com.example.lifetogether.data.repository

import com.example.lifetogether.data.remote.FirestoreDataSource
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.AdminRepository
import com.example.lifetogether.domain.repository.ListRepository
import javax.inject.Inject

class RemoteAdminRepositoryImpl @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource,
): AdminRepository {
    override suspend fun deleteCategory(
        category: Category,
    ): ResultListener {
        return firestoreDataSource.deleteCategory(category)
    }
    suspend fun addCategory(
        category: Category,
    ): ResultListener {
        return firestoreDataSource.addCategory(category)
    }
}
