package com.example.lifetogether.ui.feature.mealPlanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.session.authenticatedUserOrNull
import com.example.lifetogether.domain.repository.MealPlannerRepository
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val mealPlannerRepository: MealPlannerRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _focusDate = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val contentState: StateFlow<MealPlannerUiState.Content?> = sessionRepository.sessionState
        .map { state ->
            state.authenticatedUserOrNull?.familyId
        }
        .distinctUntilChanged()
        .flatMapLatest { familyId ->
            if (familyId == null) {
                flowOf(null)
            } else {
                observeMealPlannerContent(familyId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val uiState: StateFlow<MealPlannerUiState> = combine(
        contentState,
        _focusDate,
    ) { content, focusDate ->
        content?.copy(focusDate = focusDate) ?: MealPlannerUiState.Loading
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MealPlannerUiState.Loading,
    )

    fun onUiEvent(event: MealPlannerUiEvent) {
        when (event) {
            MealPlannerUiEvent.ClearFocusDate -> clearFocusDate()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeMealPlannerContent(familyId: String): Flow<MealPlannerUiState.Content> {
        return mealPlannerRepository.observeMealPlans(familyId)
            .successData()
            .flatMapLatest { mealPlans ->
                observeRecipePrepTimes(familyId).map { recipePrepTimes ->
                    MealPlannerUiState.Content(
                        familyId = familyId,
                        mealPlans = mealPlans,
                        recipePrepTimes = recipePrepTimes,
                    )
                }
            }
    }

    private fun observeRecipePrepTimes(familyId: String): Flow<Map<String, Int>> {
        return recipeRepository.observeRecipes(familyId)
            .successData()
            .map { recipes ->
                recipes.associate { recipe -> recipe.id to recipe.preparationTimeMin }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> Flow<Result<T, com.example.lifetogether.domain.result.AppError>>.successData(): Flow<T> {
        return transformLatest { result ->
            when (result) {
                is Result.Success -> emit(result.data)
                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(UiCommand.ShowSnackbar(message = message, withDismissAction = true))
        }
    }

    fun setFocusDate(date: String) {
        _focusDate.value = date
    }

    private fun clearFocusDate() {
        _focusDate.value = null
    }
}
