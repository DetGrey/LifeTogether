package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.callback.DefaultsResultListener
import javax.inject.Inject

// TODO DELETE???
class FetchListDefaultsUseCase @Inject constructor(
    private val listRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        listName: String,
    ): DefaultsResultListener {
        return listRepository.fetchListDefaults(listName)
    }
}
