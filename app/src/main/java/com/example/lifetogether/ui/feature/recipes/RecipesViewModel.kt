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
    companion object {
        private val DEFAULT_TAG_ORDER = listOf("Simple", "Dinner", "Breakfast")
    }

    private val _uiState = MutableStateFlow<RecipesUiState>(RecipesUiState.Loading)
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
            _uiState.value = RecipesUiState.Loading
            return
        }

        observeRecipesJob = viewModelScope.launch {
            recipeRepository.observeRecipes(familyId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        allRecipes = result.data
                        val contentState = currentContentState()
                        if (contentState == null) {
                            _uiState.value = RecipesUiState.Content(
                                recipes = applyTagFilter(result.data, "All"),
                                tagsList = buildTagsList(result.data),
                                selectedTag = "All",
                            )
                        } else {
                            updateRecipesContent()
                        }
                    }

                    is Result.Failure -> {
                        allRecipes = emptyList()
                        _uiState.value = RecipesUiState.Loading
                        showError(result.error.toUserMessage())
                    }
                }
            }
        }
    }

    private fun selectTag(tag: String) {
        updateRecipesContent { state ->
            state.copy(selectedTag = tag)
        }
        updateRecipesContent()
    }

    private fun updateRecipesContent(transform: ((RecipesUiState.Content) -> RecipesUiState.Content)? = null) {
        _uiState.update { state ->
            val contentState = state as? RecipesUiState.Content ?: return@update state
            val updatedState = transform?.invoke(contentState) ?: contentState
            val filteredRecipes = applyTagFilter(allRecipes, updatedState.selectedTag)
            updatedState.copy(
                recipes = filteredRecipes,
                tagsList = buildTagsList(allRecipes),
            )
        }
    }

    private fun currentContentState(): RecipesUiState.Content? {
        return _uiState.value as? RecipesUiState.Content
    }

    private fun applyTagFilter(recipes: List<Recipe>, selectedTag: String): List<Recipe> {
        val filtered = if (selectedTag == "All") {
            recipes
        } else {
            recipes.filter { recipe ->
                recipe.tags.any { recipeTag ->
                    recipeTag.equals(selectedTag, ignoreCase = true)
                }
            }
        }
        return filtered.sortedBy { it.itemName.lowercase() }
    }

    private fun buildTagsList(recipes: List<Recipe>): List<String> {
        val tags = linkedSetOf<String>()
        recipes.forEach { recipe ->
            recipe.tags.forEach { tag ->
                if (tag.isNotBlank()) {
                    tags += tag.normalizedTag()
                }
            }
        }

        val prioritizedTags = mutableListOf("All")
        tags.remove("All")
        DEFAULT_TAG_ORDER.forEach { tag ->
            if (tags.remove(tag)) {
                prioritizedTags += tag
            }
        }

        return prioritizedTags + tags.toList()
    }

    private fun String.normalizedTag(): String {
        return trim()
            .lowercase()
            .replaceFirstChar { character ->
                character.uppercase()
            }
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

}
