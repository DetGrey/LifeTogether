package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class MoveMediaToAlbumUseCase @Inject constructor(
    private val remoteListRepository: RemoteListRepositoryImpl,
) {
    suspend operator fun invoke(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): Result<Unit, String> {
        return remoteListRepository.moveMediaToAlbum(mediaIdList, newAlbumId, oldAlbumId)
    }
}
