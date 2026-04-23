package com.example.lifetogether.ui.feature.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val recipeRepository: RecipeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RecipesUiState())
    val uiState: StateFlow<RecipesUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private var observeRecipesJob: Job? = null
    private var currentFamilyId: String? = null
    private var allRecipes: List<Recipe> = emptyList()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != currentFamilyId) {
                    currentFamilyId = newFamilyId
                    observeRecipes(newFamilyId)
                }
            }
        }
    }

    fun onEvent(event: RecipesUiEvent) {
        when (event) {
            is RecipesUiEvent.TagSelected -> selectTag(event.tag)
        }
    }

    private fun observeRecipes(familyId: String?) {
        observeRecipesJob?.cancel()

        if (familyId.isNullOrBlank()) {
            allRecipes = emptyList()
            updateUiState {
                it.copy(
                    recipes = emptyList(),
                    tagsList = listOf("All"),
                    selectedTag = "All",
                )
            }
            return
        }

        observeRecipesJob = viewModelScope.launch {
            recipeRepository.observeRecipes(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        allRecipes = result.data
                        updateRecipesState()
                    }

                    is Result.Failure -> {
                        allRecipes = emptyList()
                        updateUiState {
                            it.copy(
                                recipes = emptyList(),
                                tagsList = listOf("All"),
                                selectedTag = "All",
                            )
                        }
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun selectTag(tag: String) {
        updateUiState { state ->
            state.copy(selectedTag = tag)
        }
        updateRecipesState()
    }

    private fun updateRecipesState() {
        val state = _uiState.value
        val filteredRecipes = if (state.selectedTag == "All") {
            allRecipes
        } else {
            allRecipes.filter { recipe ->
                recipe.tags.any { recipeTag ->
                    recipeTag.equals(state.selectedTag, ignoreCase = true)
                }
            }
        }.sortedBy { it.itemName.lowercase() }

        updateUiState {
            it.copy(
                recipes = filteredRecipes,
                tagsList = buildTagsList(allRecipes),
            )
        }
    }

    private fun buildTagsList(recipes: List<Recipe>): List<String> {
        val tags = linkedSetOf("All")
        recipes.forEach { recipe ->
            recipe.tags.forEach { tag ->
                tags += tag.replaceFirstChar { character ->
                    character.uppercase()
                }
            }
        }
        return tags.toList()
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }

    private fun updateUiState(transform: (RecipesUiState) -> RecipesUiState) {
        _uiState.update(transform)
    }
}
