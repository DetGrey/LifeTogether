package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.AppErrors

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.data.local.source.CategoryLocalDataSource
import com.example.lifetogether.data.remote.GroceryFirestoreDataSource
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.repository.CategoryRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryLocalDataSource: CategoryLocalDataSource,
    private val groceryFirestoreDataSource: GroceryFirestoreDataSource,
): CategoryRepository {
    override fun getCategories(): Flow<Result<List<Category>, AppError>> {
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
                Result.Failure(AppErrors.fromThrowable(e))
            }
        }
    }

    override fun syncCategoriesFromRemote(): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.categoriesSnapshotListener().map { result ->
            when (result) {
                is Result.Success -> runCatching {
                    categoryLocalDataSource.updateCategories(result.data)
                    Result.Success(Unit)
                }.getOrElse { error ->
                    Result.Failure(AppErrors.fromThrowable(error))
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }
}
