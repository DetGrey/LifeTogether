package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.domain.listener.GrocerySuggestionsListener
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FetchGrocerySuggestionsUseCase @Inject constructor(
    private val groceryRepository: GroceryRepository
) {
    operator fun invoke(): Flow<GrocerySuggestionsListener> {
        println("FetchGrocerySuggestionsUseCase invoked")
        return groceryRepository.getGrocerySuggestions().map {
            when (it) {
                is Result.Success -> GrocerySuggestionsListener.Success(it.data)
                is Result.Failure -> GrocerySuggestionsListener.Failure(it.error)
            }
        }
    }
}
