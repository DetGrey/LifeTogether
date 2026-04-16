package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.domain.listener.CategoriesListener
import com.example.lifetogether.domain.repository.CategoryRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FetchCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    operator fun invoke(): Flow<CategoriesListener> {
        println("FetchCategoriesUseCase invoked")
        return categoryRepository.getCategories().map {
            when (it) {
                is Result.Success -> CategoriesListener.Success(it.data)
                is Result.Failure -> CategoriesListener.Failure(it.error)
            }
        }
    }
}
