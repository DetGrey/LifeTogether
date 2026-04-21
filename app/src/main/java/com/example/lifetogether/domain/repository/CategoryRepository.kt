package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategories(): Flow<Result<List<Category>, AppError>>
    fun syncCategoriesFromRemote(): Flow<Result<Unit, AppError>>
}
