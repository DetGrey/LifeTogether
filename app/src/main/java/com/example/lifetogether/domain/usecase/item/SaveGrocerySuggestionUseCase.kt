package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteAdminRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.GrocerySuggestion
import javax.inject.Inject

class SaveGrocerySuggestionUseCase @Inject constructor(
    private val remoteAdminRepositoryImpl: RemoteAdminRepositoryImpl,
) {
    suspend operator fun invoke(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        return remoteAdminRepositoryImpl.saveGrocerySuggestion(grocerySuggestion)
    }
}
