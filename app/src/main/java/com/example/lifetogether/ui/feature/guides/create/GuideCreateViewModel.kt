package com.example.lifetogether.ui.feature.guides.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.logic.GuideRoundGrouping
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GuideCreateViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val guideRepository: GuideRepository,
) : ViewModel() {
    private var familyId: String? = null
    private var uid: String? = null

    private val _uiState = MutableStateFlow(GuideCreateUiState())
    val uiState: StateFlow<GuideCreateUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<GuideCreateCommand>(Channel.BUFFERED)
    val commands: Flow<GuideCreateCommand> = _commands.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                familyId = authenticated?.user?.familyId
                uid = authenticated?.user?.uid
                if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                }
            }
        }
    }

    fun onEvent(event: GuideCreateUiEvent) {
        when (event) {
            is GuideCreateUiEvent.TitleChanged -> updateState { it.copy(title = event.value) }
            is GuideCreateUiEvent.DescriptionChanged -> updateState { it.copy(description = event.value) }
            is GuideCreateUiEvent.VisibilityChanged -> updateState { it.copy(visibility = event.value) }
            is GuideCreateUiEvent.AddSectionRequested -> addSection(event.title, event.amount)
            is GuideCreateUiEvent.AddStepRequested -> addStep(event.sectionId, event.content, event.type)
            GuideCreateUiEvent.SaveClicked -> saveGuide()
        }
    }

    private fun addSection(
        sectionTitle: String,
        amount: Int = 1,
    ) {
        val currentSections = _uiState.value.sections
        val normalizedTitle = sectionTitle.trim().ifBlank {
            "Section ${currentSections.size + 1}"
        }
        val normalizedAmount = amount.coerceAtLeast(1)
        updateState {
            it.copy(
                sections = currentSections + GuideSection(
                    id = UUID.randomUUID().toString(),
                    orderNumber = currentSections.size + 1,
                    title = normalizedTitle,
                    amount = normalizedAmount,
                    steps = emptyList(),
                ),
            )
        }
    }

    private fun addStep(
        sectionId: String,
        content: String,
        type: GuideStepType = GuideStepType.NUMBERED,
    ) {
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) return

        updateState { state ->
            state.copy(
                sections = state.sections.map { section ->
                    if (section.id != sectionId) {
                        section
                    } else {
                        val newSteps = when (type) {
                            GuideStepType.SUBSECTION -> listOf(
                                GuideStep(
                                    id = UUID.randomUUID().toString(),
                                    type = type,
                                    title = normalizedContent,
                                ),
                            )

                            GuideStepType.ROUND -> expandRoundDraft(normalizedContent)

                            else -> listOf(
                                GuideStep(
                                    id = UUID.randomUUID().toString(),
                                    type = type,
                                    content = normalizedContent,
                                ),
                            )
                        }
                        section.copy(steps = section.steps + newSteps)
                    }
                },
            )
        }
    }

    private fun expandRoundDraft(draft: String): List<GuideStep> {
        val parsedPrefix = GuideRoundGrouping.parseRoundPrefix(draft) ?: return listOf(
            GuideStep(
                id = UUID.randomUUID().toString(),
                type = GuideStepType.ROUND,
                content = draft,
            ),
        )
        val (roundRange, sharedContent) = parsedPrefix
        return roundRange.map { roundNumber ->
            GuideStep(
                id = UUID.randomUUID().toString(),
                type = GuideStepType.ROUND,
                name = "R$roundNumber",
                content = sharedContent,
            )
        }
    }

    private fun saveGuide() {
        val activeFamilyId = familyId
        val activeUid = uid
        val currentState = _uiState.value

        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) {
            showError("Please connect to a family first")
            return
        }
        if (currentState.title.trim().isEmpty()) {
            showError("Please enter a title")
            return
        }

        val guide = buildGuideFromState(
            familyId = activeFamilyId,
            uid = activeUid,
            state = currentState,
        )

        persistGuide(guide)
    }

    private fun buildGuideFromState(
        familyId: String,
        uid: String,
        state: GuideCreateUiState,
    ): Guide {
        val normalizedSections = state.sections
            .mapIndexed { index, section ->
                GuideProgress.updateSectionCompletion(
                    section.copy(orderNumber = index + 1),
                )
            }

        return Guide(
            familyId = familyId,
            itemName = state.title.trim(),
            description = state.description.trim(),
            lastUpdated = Date(),
            visibility = state.visibility,
            ownerUid = uid,
            started = false,
            sections = normalizedSections,
            resume = null,
        )
    }

    private fun persistGuide(guide: Guide) {
        viewModelScope.launch {
            when (val result = guideRepository.saveGuide(guide)) {
                is Result.Success -> {
                    _commands.send(GuideCreateCommand.NavigateToGuideDetails(result.data))
                }
                is Result.Failure -> showError(result.error.toUserMessage())
            }
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

    private fun updateState(transform: (GuideCreateUiState) -> GuideCreateUiState) {
        _uiState.update(transform)
    }
}
