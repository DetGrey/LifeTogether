package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.AdminRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import javax.inject.Inject

class SaveGrocerySuggestionUseCase @Inject constructor(
    private val adminRepositoryImpl: AdminRepositoryImpl,
) {
    suspend operator fun invoke(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        return adminRepositoryImpl.saveGrocerySuggestion(grocerySuggestion)
    }
}
