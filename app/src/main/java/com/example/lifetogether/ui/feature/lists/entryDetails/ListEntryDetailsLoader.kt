package com.example.lifetogether.ui.feature.lists.entryDetails

import com.example.lifetogether.domain.model.lists.ListType
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

data class ListEntryDetailsLoadSnapshot(
    val familyId: String?,
    val state: ListEntryDetailsLoadState,
)

sealed interface ListEntryDetailsLoadState {
    data object Loading : ListEntryDetailsLoadState

    data class Content(
        val details: EntryDetailsContent,
        val isNewEntry: Boolean,
    ) : ListEntryDetailsLoadState

    data class Error(
        val message: String,
    ) : ListEntryDetailsLoadState
}

class ListEntryDetailsLoader @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val userListRepository: UserListRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(listId: String, entryId: String?): Flow<ListEntryDetailsLoadSnapshot> {
        return sessionRepository.sessionState
            .map { it.authenticatedUserOrNull?.familyId }
            .distinctUntilChanged()
            .flatMapLatest { familyId ->
                if (familyId == null) {
                    flowOf(ListEntryDetailsLoadSnapshot(familyId = null, state = ListEntryDetailsLoadState.Loading))
                } else {
                    userListRepository.observeUserLists(familyId).flatMapLatest { result ->
                        when (result) {
                            is Result.Success -> {
                                val list = result.data.firstOrNull { it.id == listId }
                                    ?: return@flatMapLatest flowOf(
                                        ListEntryDetailsLoadSnapshot(
                                            familyId = familyId,
                                            state = ListEntryDetailsLoadState.Loading,
                                        ),
                                    )

                                if (entryId == null) {
                                    flowOf(
                                        ListEntryDetailsLoadSnapshot(
                                            familyId = familyId,
                                            state = ListEntryDetailsLoadState.Content(
                                                details = blankDetailsFor(list.type),
                                                isNewEntry = true,
                                            ),
                                        ),
                                    )
                                } else {
                                    observeEntry(list.type, entryId).map { entryResult ->
                                        entryResult.toLoadSnapshot(familyId)
                                    }
                                }
                            }

                            is Result.Failure -> flowOf(
                                ListEntryDetailsLoadSnapshot(
                                    familyId = familyId,
                                    state = ListEntryDetailsLoadState.Error(result.error.toUserMessage()),
                                ),
                            )
                        }
                    }
                }
            }
    }

    private fun observeEntry(
        listType: ListType,
        entryId: String,
    ): Flow<Result<EntryDetailsContent, AppError>> {
        return when (listType) {
            ListType.ROUTINE -> userListRepository.observeRoutineListEntry(entryId).map { result ->
                result.mapData { EntryDetailsContent.Routine.from(it) }
            }

            ListType.WISH_LIST -> userListRepository.observeWishListEntry(entryId).map { result ->
                result.mapData { EntryDetailsContent.Wish.from(it) }
            }

            ListType.NOTES -> userListRepository.observeNoteEntry(entryId).map { result ->
                result.mapData { EntryDetailsContent.Note.from(it) }
            }

            ListType.CHECKLIST -> userListRepository.observeChecklistEntry(entryId).map { result ->
                result.mapData { EntryDetailsContent.Checklist.from(it) }
            }

            ListType.MEAL_PLANNER -> userListRepository.observeMealPlanEntry(entryId).map { result ->
                result.mapData { EntryDetailsContent.Meal.from(it) }
            }
        }
    }

    private fun blankDetailsFor(listType: ListType): EntryDetailsContent {
        return when (listType) {
            ListType.ROUTINE -> EntryDetailsContent.Routine.blank()
            ListType.WISH_LIST -> EntryDetailsContent.Wish.blank()
            ListType.NOTES -> EntryDetailsContent.Note.blank()
            ListType.CHECKLIST -> EntryDetailsContent.Checklist.blank()
            ListType.MEAL_PLANNER -> EntryDetailsContent.Meal.blank()
        }
    }

    private fun <T> Result<T, AppError>.mapData(transform: (T) -> EntryDetailsContent): Result<EntryDetailsContent, AppError> {
        return when (this) {
            is Result.Success -> Result.Success(transform(data))
            is Result.Failure -> Result.Failure(error)
        }
    }

    private fun Result<EntryDetailsContent, AppError>.toLoadSnapshot(familyId: String): ListEntryDetailsLoadSnapshot {
        return when (this) {
            is Result.Success -> ListEntryDetailsLoadSnapshot(
                familyId = familyId,
                state = ListEntryDetailsLoadState.Content(
                    details = data,
                    isNewEntry = false,
                ),
            )

            is Result.Failure -> ListEntryDetailsLoadSnapshot(
                familyId = familyId,
                state = ListEntryDetailsLoadState.Error(error.toUserMessage()),
            )
        }
    }
}
