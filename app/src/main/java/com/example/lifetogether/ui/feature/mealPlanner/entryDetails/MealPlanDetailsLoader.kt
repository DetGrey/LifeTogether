package com.example.lifetogether.ui.feature.mealPlanner.entryDetails

import com.example.lifetogether.domain.model.mealplanner.MealPlan
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.repository.MealPlannerRepository
import com.example.lifetogether.domain.repository.SessionRepository
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

data class MealPlanDetailsLoadSnapshot(
    val familyId: String?,
    val state: MealPlanDetailsLoadState,
)

sealed interface MealPlanDetailsLoadState {
    data object Loading : MealPlanDetailsLoadState
    data object Deleted : MealPlanDetailsLoadState

    data class Content(
        val form: MealPlanFormState,
        val isNewEntry: Boolean,
    ) : MealPlanDetailsLoadState

    data class Error(
        val message: String,
    ) : MealPlanDetailsLoadState
}

class MealPlanDetailsLoader @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val mealPlannerRepository: MealPlannerRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observe(mealPlanId: String?): Flow<MealPlanDetailsLoadSnapshot> {
        return sessionRepository.sessionState
            .map { it.authenticatedUserOrNull?.familyId }
            .distinctUntilChanged()
            .flatMapLatest { familyId ->
                if (familyId == null) {
                    flowOf(MealPlanDetailsLoadSnapshot(familyId = null, state = MealPlanDetailsLoadState.Loading))
                } else {
                    if (mealPlanId == null) {
                        flowOf(
                            MealPlanDetailsLoadSnapshot(
                                familyId = familyId,
                                state = MealPlanDetailsLoadState.Content(
                                    form = MealPlanFormState(),
                                    isNewEntry = true,
                                ),
                            ),
                        )
                    } else {
                        mealPlannerRepository.observeMealPlan(mealPlanId).map { result ->
                            result.mapToForm().toLoadSnapshot(familyId)
                        }
                    }
                }
            }
    }

    private fun Result<MealPlan, AppError>.mapToForm(): Result<MealPlanFormState, AppError> {
        return when (this) {
            is Result.Success -> Result.Success(MealPlanFormState.from(data))
            is Result.Failure -> Result.Failure(error)
        }
    }

    private fun Result<MealPlanFormState, AppError>.toLoadSnapshot(familyId: String): MealPlanDetailsLoadSnapshot {
        return when (this) {
            is Result.Success -> MealPlanDetailsLoadSnapshot(
                familyId = familyId,
                state = MealPlanDetailsLoadState.Content(
                    form = data,
                    isNewEntry = false,
                ),
            )

            is Result.Failure -> MealPlanDetailsLoadSnapshot(
                familyId = familyId,
                state = if (error is AppError.NotFound) {
                    MealPlanDetailsLoadState.Deleted
                } else {
                    MealPlanDetailsLoadState.Error(error.toUserMessage())
                },
            )
        }
    }
}
