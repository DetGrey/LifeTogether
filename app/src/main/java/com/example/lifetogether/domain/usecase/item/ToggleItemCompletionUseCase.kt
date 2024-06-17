package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.ListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.GroceryItem

class ToggleItemCompletionUseCase {
    private val listRepository = ListRepositoryImpl()
    suspend operator fun invoke(
        item: GroceryItem,
    ): ResultListener {
        return listRepository.toggleItemCompletionInGroceryList(item)
    }
}
