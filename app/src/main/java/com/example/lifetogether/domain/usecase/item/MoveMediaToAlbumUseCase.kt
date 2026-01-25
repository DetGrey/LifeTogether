package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.listener.ResultListener
import javax.inject.Inject

class MoveMediaToAlbumUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): ResultListener {
        return remoteListRepository.moveMediaToAlbum(mediaIdList, newAlbumId, oldAlbumId)
    }
}
