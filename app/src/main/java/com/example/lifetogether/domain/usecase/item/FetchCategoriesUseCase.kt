package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.callback.CategoriesListener
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchCategoriesUseCase @Inject constructor(
    private val localListRepositoryImpl: LocalListRepositoryImpl,
) {
    operator fun invoke(): Flow<CategoriesListener> {
        println("FetchCategoriesUseCase invoked")
        return localListRepositoryImpl.getCategories()
    }
}
