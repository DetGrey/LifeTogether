package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.AdminRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import javax.inject.Inject

class DeleteGrocerySuggestionUseCase @Inject constructor(
    private val adminRepositoryImpl: AdminRepositoryImpl,
) {
    suspend operator fun invoke(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        println("Inside DeleteGrocerySuggestionUseCase and trying to delete a grocerySuggestion")
        val firestoreResult = adminRepositoryImpl.deleteGrocerySuggestion(grocerySuggestion)
        return firestoreResult
    }
}
