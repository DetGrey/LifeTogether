package com.example.lifetogether.ui.feature.guides.edit

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
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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

@HiltViewModel(assistedFactory = GuideEditViewModel.Factory::class)
class GuideEditViewModel @AssistedInject constructor(
    @Assisted val guideId: String?,
    private val sessionRepository: SessionRepository,
    private val guideRepository: GuideRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(guideId: String?): GuideEditViewModel
    }

    private var familyId: String? = null
    private var uid: String? = null
    private var originalGuide: Guide? = null

    private val initialState: GuideEditUiState = if (guideId != null) {
        GuideEditUiState.Loading
    } else {
        GuideEditUiState.Content()
    }

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<GuideEditUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<GuideEditCommand>(Channel.BUFFERED)
    val commands: Flow<GuideEditCommand> = _commands.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                val newFamilyId = authenticated?.user?.familyId
                val newUid = authenticated?.user?.uid
                if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                } else if (newFamilyId != null && newUid != null) {
                    val firstLoad = familyId == null && uid == null
                    familyId = newFamilyId
                    uid = newUid
                    if (firstLoad && guideId != null) {
                        loadGuide(newFamilyId, guideId, newUid)
                    }
                }
            }
        }
    }

    private fun loadGuide(familyId: String, guideId: String, uid: String) {
        viewModelScope.launch {
            guideRepository.observeGuideById(familyId, guideId, uid).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val guide = result.data
                        if (originalGuide == null) {
                            originalGuide = guide
                            _uiState.value = GuideEditUiState.Content(
                                title = guide.itemName,
                                description = guide.description,
                                visibility = guide.visibility,
                                sections = guide.sections,
                                isEditMode = true,
                            )
                        }
                        return@collect
                    }
                    is Result.Failure -> {
                        showError(result.error.toUserMessage())
                        return@collect
                    }
                }
            }
        }
    }

    fun onEvent(event: GuideEditUiEvent) {
        when (event) {
            is GuideEditUiEvent.TitleChanged -> updateContent { it.copy(title = event.value) }
            is GuideEditUiEvent.DescriptionChanged -> updateContent { it.copy(description = event.value) }
            is GuideEditUiEvent.VisibilityChanged -> updateContent { it.copy(visibility = event.value) }
            is GuideEditUiEvent.AddSectionRequested -> addSection(event.title, event.amount)
            is GuideEditUiEvent.DeleteSectionRequested -> deleteSection(event.sectionId)
            is GuideEditUiEvent.SectionMoved -> moveSection(event.fromIndex, event.toIndex)
            is GuideEditUiEvent.AddStepRequested -> addStep(event.sectionId, event.content, event.type)
            is GuideEditUiEvent.DeleteStepRequested -> deleteStep(event.sectionId, event.stepId)
            is GuideEditUiEvent.StepMoved -> moveStep(event.sectionId, event.fromIndex, event.toIndex)
            is GuideEditUiEvent.StepDraftChanged -> updateContent {
                it.copy(stepDrafts = it.stepDrafts + (event.sectionId to event.value))
            }
            is GuideEditUiEvent.StepTypeDraftChanged -> updateContent {
                it.copy(stepTypeDrafts = it.stepTypeDrafts + (event.sectionId to event.type))
            }
            GuideEditUiEvent.SaveClicked -> saveGuide()
            GuideEditUiEvent.DiscardClicked -> {
                if (_uiState.value is GuideEditUiState.Loading) {
                    viewModelScope.launch { _commands.send(GuideEditCommand.NavigateBack) }
                } else {
                    updateContent { it.copy(showDiscardDialog = true) }
                }
            }
            GuideEditUiEvent.DismissDiscardDialog -> updateContent { it.copy(showDiscardDialog = false) }
            GuideEditUiEvent.ConfirmDiscard -> viewModelScope.launch {
                _commands.send(GuideEditCommand.NavigateBack)
            }
        }
    }

    private fun addSection(sectionTitle: String, amount: Int = 1) {
        val currentSections = currentContent()?.sections ?: return
        val normalizedTitle = sectionTitle.trim().ifBlank {
            "Section ${currentSections.size + 1}"
        }
        val normalizedAmount = amount.coerceAtLeast(1)
        updateContent {
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

    private fun deleteSection(sectionId: String) {
        updateContent { state ->
            state.copy(
                sections = state.sections
                    .filter { it.id != sectionId }
                    .mapIndexed { index, section -> section.copy(orderNumber = index + 1) },
                stepDrafts = state.stepDrafts - sectionId,
                stepTypeDrafts = state.stepTypeDrafts - sectionId,
            )
        }
    }

    private fun moveSection(fromIndex: Int, toIndex: Int) {
        updateContent { state ->
            val mutable = state.sections.toMutableList()
            mutable.add(toIndex, mutable.removeAt(fromIndex))
            state.copy(
                sections = mutable.mapIndexed { index, section -> section.copy(orderNumber = index + 1) },
            )
        }
    }

    private fun addStep(sectionId: String, content: String, type: GuideStepType) {
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) {
            showInfo("Type some content before adding a step")
            return
        }

        updateContent { state ->
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
                                    name = "",
                                    title = normalizedContent,
                                    content = "",
                                    subSteps = emptyList(),
                                ),
                            )
                            GuideStepType.ROUND -> expandRoundDraft(normalizedContent)
                            else -> listOf(
                                GuideStep(
                                    id = UUID.randomUUID().toString(),
                                    type = type,
                                    name = "",
                                    title = "",
                                    content = normalizedContent,
                                    subSteps = emptyList(),
                                ),
                            )
                        }
                        section.copy(steps = section.steps + newSteps)
                    }
                },
                stepDrafts = state.stepDrafts + (sectionId to ""),
            )
        }
    }

    private fun deleteStep(sectionId: String, stepId: String) {
        updateContent { state ->
            state.copy(
                sections = state.sections.map { section ->
                    if (section.id != sectionId) section
                    else section.copy(steps = section.steps.filter { it.id != stepId })
                },
            )
        }
    }

    private fun moveStep(sectionId: String, fromIndex: Int, toIndex: Int) {
        updateContent { state ->
            state.copy(
                sections = state.sections.map { section ->
                    if (section.id != sectionId) section
                    else {
                        val mutable = section.steps.toMutableList()
                        mutable.add(toIndex, mutable.removeAt(fromIndex))
                        section.copy(steps = mutable)
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
                name = "",
                title = "",
                content = draft,
                subSteps = emptyList(),
            ),
        )
        val (roundRange, sharedContent) = parsedPrefix
        return roundRange.map { roundNumber ->
            GuideStep(
                id = UUID.randomUUID().toString(),
                type = GuideStepType.ROUND,
                name = "R$roundNumber",
                title = "",
                content = sharedContent,
                subSteps = emptyList(),
            )
        }
    }

    private fun saveGuide() {
        val activeFamilyId = familyId
        val activeUid = uid
        val currentState = currentContent() ?: return

        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) {
            showError("Please connect to a family first")
            return
        }
        if (currentState.title.trim().isEmpty()) {
            showError("Please enter a title")
            return
        }

        if (currentState.isEditMode) {
            val base = originalGuide ?: run {
                showError("Could not load original guide")
                return
            }
            persistUpdate(buildUpdatedGuide(base, currentState))
        } else {
            persistNew(buildNewGuide(activeFamilyId, activeUid, currentState))
        }
    }

    private fun buildNewGuide(
        familyId: String,
        uid: String,
        state: GuideEditUiState.Content,
    ): Guide {
        val normalizedSections = state.sections.mapIndexed { index, section ->
            GuideProgress.updateSectionCompletion(section.copy(orderNumber = index + 1))
        }
        return Guide(
            id = UUID.randomUUID().toString(),
            familyId = familyId,
            itemName = state.title.trim(),
            description = state.description.trim(),
            lastUpdated = Date(),
            visibility = state.visibility,
            ownerUid = uid,
            contentVersion = 1L,
            started = false,
            sections = normalizedSections,
            resume = null,
        )
    }

    private fun buildUpdatedGuide(base: Guide, state: GuideEditUiState.Content): Guide {
        val normalizedSections = state.sections.mapIndexed { index, section ->
            GuideProgress.updateSectionCompletion(section.copy(orderNumber = index + 1))
        }
        return base.copy(
            itemName = state.title.trim(),
            description = state.description.trim(),
            lastUpdated = Date(),
            visibility = state.visibility,
            contentVersion = base.contentVersion + 1,
            sections = normalizedSections,
        )
    }

    private fun persistNew(guide: Guide) {
        updateContent { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = guideRepository.saveGuide(guide)) {
                is Result.Success -> {
                    _commands.send(GuideEditCommand.NavigateToGuideDetails(result.data))
                }
                is Result.Failure -> {
                    updateContent { it.copy(isSaving = false) }
                    showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun persistUpdate(guide: Guide) {
        updateContent { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = guideRepository.updateGuide(guide)) {
                is Result.Success -> {
                    _commands.send(GuideEditCommand.NavigateToGuideDetails(guide.id))
                }
                is Result.Failure -> {
                    updateContent { it.copy(isSaving = false) }
                    showError(result.error.toUserMessage())
                }
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

    private fun showInfo(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                    severity = SnackbarSeverity.Info,
                ),
            )
        }
    }

    private fun currentContent(): GuideEditUiState.Content? =
        _uiState.value as? GuideEditUiState.Content

    private fun updateContent(transform: (GuideEditUiState.Content) -> GuideEditUiState.Content) {
        _uiState.update { state ->
            if (state is GuideEditUiState.Content) transform(state) else state
        }
    }
}
