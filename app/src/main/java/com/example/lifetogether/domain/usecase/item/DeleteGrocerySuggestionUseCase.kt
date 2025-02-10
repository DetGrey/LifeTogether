package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteAdminRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import javax.inject.Inject

class DeleteGrocerySuggestionUseCase @Inject constructor(
    private val remoteAdminRepositoryImpl: RemoteAdminRepositoryImpl,
) {
    suspend operator fun invoke(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        println("Inside DeleteGrocerySuggestionUseCase and trying to delete a grocerySuggestion")
        val firestoreResult = remoteAdminRepositoryImpl.deleteGrocerySuggestion(grocerySuggestion)
        return firestoreResult
    }
}
