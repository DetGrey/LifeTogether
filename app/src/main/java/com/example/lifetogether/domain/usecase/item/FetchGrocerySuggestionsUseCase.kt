package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FetchGrocerySuggestionsUseCase @Inject constructor(
    private val localListRepositoryImpl: LocalListRepositoryImpl,
) {
    operator fun invoke(): Flow<GrocerySuggestionsListener> {
        println("FetchGrocerySuggestionsUseCase invoked")
        return localListRepositoryImpl.getGrocerySuggestions()
    }
}
