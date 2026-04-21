package com.example.lifetogether.data.repository

import com.example.lifetogether.data.logic.appResultOf

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
            appResultOf {
                list.map { category ->
                    Category(
                        emoji = category.emoji,
                        name = category.name,
                    )
                }
            }
        }
    }

    override fun syncCategoriesFromRemote(): Flow<Result<Unit, AppError>> {
        return groceryFirestoreDataSource.categoriesSnapshotListener().map { result ->
            when (result) {
                is Result.Success -> appResultOf {
                    categoryLocalDataSource.updateCategories(result.data)
                }
                is Result.Failure -> Result.Failure(result.error)
            }
        }
    }
}
