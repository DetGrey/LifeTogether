package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteAdminRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import javax.inject.Inject

class UpdateGrocerySuggestionUseCase @Inject constructor(
    private val remoteAdminRepositoryImpl: RemoteAdminRepositoryImpl,
) {
    suspend operator fun invoke(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        return remoteAdminRepositoryImpl.updateGrocerySuggestion(grocerySuggestion)
    }
}
