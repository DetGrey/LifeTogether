package com.example.lifetogether.domain.usecase.image

import com.example.lifetogether.domain.model.sealed.ImageState
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveImageStateUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    companion object {
        private const val NO_BYTE_ARRAY_MESSAGE = "No ByteArray found"
    }

    operator fun invoke(imageType: ImageType?): Flow<ImageState> {
        if (!isValidImageInput(imageType)) {
            return flowOf(ImageState.Empty)
        }

        return imageRepository.observeImageByteArray(imageType!!).map { result ->
            when (result) {
                is Result.Success -> ImageState.Loaded(result.data)
                is Result.Failure -> {
                    if (result.error.message == NO_BYTE_ARRAY_MESSAGE) {
                        ImageState.Empty
                    } else {
                        ImageState.Error(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun isValidImageInput(imageType: ImageType?): Boolean {
        return when (imageType) {
            null -> false
            is ImageType.ProfileImage -> imageType.uid.isNotBlank()
            is ImageType.FamilyImage -> imageType.familyId.isNotBlank()
            is ImageType.RecipeImage -> imageType.familyId.isNotBlank() && imageType.recipeId.isNotBlank()
            is ImageType.RoutineListEntryImage -> imageType.familyId.isNotBlank() && imageType.entryId.isNotBlank()
            is ImageType.GalleryMedia -> false
        }
    }
}