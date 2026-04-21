package com.example.lifetogether.domain.usecase.item

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.result.Result
import javax.inject.Inject

class MoveMediaToAlbumUseCase @Inject constructor(
    private val galleryRepository: GalleryRepository,
) {
    suspend operator fun invoke(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): Result<Unit, AppError> {
        return galleryRepository.moveMediaToAlbum(mediaIdList, newAlbumId, oldAlbumId)
    }
}
