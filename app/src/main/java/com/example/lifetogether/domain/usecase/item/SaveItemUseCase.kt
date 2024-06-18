package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.ListRepositoryImpl
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item

class SaveItemUseCase {
    private val listRepository = ListRepositoryImpl()
    suspend operator fun invoke(
        item: Item,
        listName: String,
    ): ResultListener {
        return listRepository.saveItem(item, listName)
    }
}
