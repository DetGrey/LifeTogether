package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.source.CategoryLocalDataSource
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.repository.CategoryRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryLocalDataSource: CategoryLocalDataSource,
): CategoryRepository {
    override fun getCategories(): Flow<Result<List<Category>, String>> {
        return categoryLocalDataSource.getCategories().map { list ->
            try {
                Result.Success(
                    list.map { category ->
                        Category(
                            emoji = category.emoji,
                            name = category.name,
                        )
                    },
                )
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown error")
            }
        }
    }
}
