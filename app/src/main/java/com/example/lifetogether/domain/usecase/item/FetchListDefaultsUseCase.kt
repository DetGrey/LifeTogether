package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.ListRepositoryImpl
import com.example.lifetogether.domain.callback.DefaultsResultListener

class FetchListDefaultsUseCase {
    private val listRepository = ListRepositoryImpl()
    suspend operator fun invoke(
        listName: String,
    ): DefaultsResultListener {
        return listRepository.fetchListDefaults(listName)
    }
}
