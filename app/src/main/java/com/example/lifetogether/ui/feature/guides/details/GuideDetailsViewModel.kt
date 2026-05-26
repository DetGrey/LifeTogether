package com.example.lifetogether.ui.feature.guides.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.GuideLeafPointer
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.ui.common.event.UiCommand
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
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
import java.util.Date

@HiltViewModel(assistedFactory = GuideDetailsViewModel.Factory::class)
class GuideDetailsViewModel @AssistedInject constructor(
    @Assisted val guideId: String,
    private val sessionRepository: SessionRepository,
    private val guideRepository: GuideRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(guideId: String): GuideDetailsViewModel
    }
    private var familyId: String? = null
    private var uid: String? = null
    private var guideJob: Job? = null

    private var pointerCacheGuide: Guide? = null
    private var pointerCacheByStepId: Map<String, GuideLeafPointer> = emptyMap()
    internal var nowProvider: () -> Date = ::Date

    private val _uiState = MutableStateFlow<GuideDetailsUiState>(GuideDetailsUiState.Loading)
    val uiState: StateFlow<GuideDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<GuideDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<GuideDetailsCommand> = _commands.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val authenticated = state as? SessionState.Authenticated
                val newFamilyId = authenticated?.user?.familyId
                val newUid = authenticated?.user?.uid
                if ((newFamilyId != null && newUid != null) &&
                    (newFamilyId != familyId || newUid != uid)
                ) {
                    familyId = newFamilyId
                    uid = newUid
                    observeGuide()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                }
            }
        }
    }

    fun onEvent(event: GuideDetailsUiEvent) {
        when (event) {
            GuideDetailsUiEvent.StartOrContinueClicked -> onStartOrContinue()
            GuideDetailsUiEvent.ResetAllProgressClicked -> resetAllProgress()
            GuideDetailsUiEvent.ToggleVisibilityClicked -> toggleVisibility()
            GuideDetailsUiEvent.DeleteGuideClicked -> deleteGuide()
            GuideDetailsUiEvent.CompleteAndGoToSelectedStepClicked -> completeAndGoToSelectedStep()
            is GuideDetailsUiEvent.SelectJumpOption -> selectJumpOption(event.optionKey)
            is GuideDetailsUiEvent.ToggleSectionExpanded -> toggleSectionExpanded(event.sectionKey)
            is GuideDetailsUiEvent.SelectSectionPiece -> selectSectionPiece(
                sectionKey = event.sectionKey,
                pieceIndex = event.pieceIndex,
            )
            is GuideDetailsUiEvent.ToggleStepCompletion -> toggleStepCompletion(
                stepId = event.stepId,
                pieceIndex = event.pieceIndex,
            )
        }
    }

    private fun observeGuide() {
        val familyIdValue = familyId ?: return
        val uidValue = uid ?: return

        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            guideRepository.observeGuideById(
                familyId = familyIdValue,
                id = guideId,
                uid = uidValue,
            ).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val normalizedGuide = normalizeFetchedGuide(
                            guide = result.data,
                            fallbackGuideId = guideId,
                        )
                        applyGuideUpdate(uiGuide = normalizedGuide)
                    }
                    is Result.Failure -> {
                        if (result.error is AppError.NotFound) {
                            viewModelScope.launch { _commands.send(GuideDetailsCommand.NavigateBack) }
                        } else {
                            showError(result.error.toUserMessage())
                        }
                    }
                }
            }
        }
    }

    fun canToggleVisibility(): Boolean {
        val currentGuide = currentGuide() ?: return false
        val activeUid = uid ?: return false
        return currentGuide.ownerUid == activeUid
    }

    fun canDeleteGuide(): Boolean {
        val currentGuide = currentGuide() ?: return false
        val activeUid = uid ?: return false
        return currentGuide.ownerUid == activeUid
    }

    fun toggleVisibility() {
        val currentGuide = currentGuide() ?: return
        val activeUid = uid ?: return
        if (currentContentState()?.isUpdatingVisibility == true) return

        if (!canToggleVisibility()) {
            showError("Only the owner can change visibility")
            return
        }

        val currentGuideId = resolveGuideId(currentGuide)

        val newVisibility = if (currentGuide.visibility == Visibility.FAMILY)
            Visibility.PRIVATE else Visibility.FAMILY
        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            visibility = newVisibility,
            ownerUid = activeUid,
            lastUpdated = now(),
        )

        applyGuideUpdate(
            uiGuide = updatedGuide,
            persistedGuide = updatedGuide,
            beforePersist = { updateLoadingState(isUpdatingVisibility = true) },
            afterPersist = { updateLoadingState(isUpdatingVisibility = false) },
        )
    }

    fun deleteGuide() {
        if (currentContentState()?.isDeletingGuide == true) return

        val currentGuide = currentGuide()
        if (!canDeleteGuide()) {
            showError("Only the owner can delete this guide")
            return
        }
        val currentGuideId = resolveGuideId(currentGuide)

        viewModelScope.launch {
            updateLoadingState(isDeletingGuide = true)
            when (val result = guideRepository.deleteGuide(currentGuideId)) {
                is Result.Success -> Unit
                is Result.Failure -> showError(result.error.toUserMessage())
            }
            updateLoadingState(isDeletingGuide = false)
        }
    }

    fun toggleStepCompletion(stepId: String, pieceIndex: Int) {
        if (stepId.isBlank()) return

        val currentGuide = currentGuide() ?: return
        val currentGuideId = resolveGuideId(currentGuide)

        val pointer = pointerForStepId(
            guide = currentGuide,
            stepId = stepId,
            pieceIndex = pieceIndex,
        ) ?: return
        if (!GuideProgress.canTogglePointer(currentGuide.sections, pointer)) return

        val updatedSections = GuideProgress.applyLeafCompletion(
            sections = currentGuide.sections,
            pointer = pointer,
            completed = !GuideProgress.isPointerCompleted(currentGuide.sections, pointer),
        )

        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            started = true,
            sections = updatedSections,
            resume = GuideProgress.firstIncompletePointer(updatedSections)?.let(GuideProgress::resumeFromPointer),
            lastUpdated = now(),
        )

        applyGuideUpdate(uiGuide = updatedGuide)
        persistGuideProgress(updatedGuide)
    }

    fun onStartOrContinue() {
        val currentGuide = currentGuide() ?: return
        val currentGuideId = resolveGuideId(currentGuide)

        if (currentGuide.started) {
            viewModelScope.launch { _commands.send(GuideDetailsCommand.NavigateToGuideStepPlayer) }
            return
        }

        if (currentContentState()?.isStartingGuide == true) return

        val firstPointer = GuideProgress.defaultPointerForGuide(currentGuide)
        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            started = true,
            resume = firstPointer?.let(GuideProgress::resumeFromPointer),
            lastUpdated = now(),
        )

        updateLoadingState(isStartingGuide = true)
        applyGuideUpdate(uiGuide = updatedGuide)
        persistGuideProgress(updatedGuide) {
            updateLoadingState(isStartingGuide = false)
            viewModelScope.launch { _commands.send(GuideDetailsCommand.NavigateToGuideStepPlayer) }
        }
    }

    fun resetAllProgress() {
        val currentGuide = currentGuide() ?: return
        val currentGuideId = resolveGuideId(currentGuide)

        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            started = false,
            sections = GuideProgress.resetSectionsProgress(currentGuide.sections),
            resume = null,
            lastUpdated = now(),
        )

        applyGuideUpdate(uiGuide = updatedGuide)
        persistGuideProgress(updatedGuide)
    }

    fun completeAndGoToSelectedStep() {
        val currentGuide = currentGuide() ?: return
        val selectedOptionKey = currentContentState()?.selectedJumpOptionKey ?: return
        val jumpOption = currentContentState()?.jumpOptions
            ?.firstOrNull { it.key == selectedOptionKey }
            ?: return
        val currentGuideId = resolveGuideId(currentGuide)

        val resetSections = GuideProgress.resetSectionsProgress(currentGuide.sections)
        val updatedSections = completeGuideUntilPointer(
            sections = resetSections,
            targetPointer = jumpOption.pointer,
        )
        val updatedGuide = currentGuide.copy(
            id = currentGuideId,
            started = true,
            sections = updatedSections,
            resume = GuideProgress.resumeFromPointer(jumpOption.pointer),
            lastUpdated = now(),
        )

        applyGuideUpdate(uiGuide = updatedGuide)
        focusPointer(jumpOption.pointer, updatedGuide)
        persistGuideProgress(updatedGuide)
    }

    fun toggleSectionExpanded(sectionKey: String) {
        if (sectionKey.isBlank()) return
        updateContentState { state ->
            val isExpanded = state.sectionExpandedState[sectionKey] ?: true
            state.copy(
                sectionExpandedState = state.sectionExpandedState + (sectionKey to !isExpanded),
            )
        }
    }

    fun selectSectionPiece(sectionKey: String, pieceIndex: Int) {
        if (sectionKey.isBlank() || pieceIndex < 0) return
        val guide = currentGuide() ?: return
        val section = guide.sections
            .withIndex()
            .firstOrNull { (index, section) -> guideSectionKey(section, index) == sectionKey }
            ?.value
            ?: return
        val maxIndex = section.pieces.coerceAtLeast(1) - 1
        val normalizedPieceIndex = pieceIndex.coerceIn(0, maxIndex)
        updateContentState { state ->
            state.copy(
                selectedSectionPieceState = state.selectedSectionPieceState + (sectionKey to normalizedPieceIndex),
            )
        }
    }

    fun selectJumpOption(optionKey: String) {
        if (optionKey.isBlank()) return
        updateContentState { state ->
            if (state.jumpOptions.none { it.key == optionKey }) {
                state
            } else {
                state.copy(selectedJumpOptionKey = optionKey)
            }
        }
    }

    private fun applyGuideUpdate(
        uiGuide: Guide,
        persistedGuide: Guide? = null,
        beforePersist: (() -> Unit)? = null,
        afterPersist: (() -> Unit)? = null,
        onPersistSuccess: (() -> Unit)? = null,
    ) {
        updateGuideState(uiGuide)
        if (persistedGuide == null) return

        beforePersist?.invoke()
        persistGuideUpdate(
            guide = persistedGuide,
            onSuccess = {
                afterPersist?.invoke()
                onPersistSuccess?.invoke()
            },
            onFailure = { afterPersist?.invoke() },
        )
    }

    private fun persistGuideUpdate(guide: Guide, onSuccess: () -> Unit, onFailure: () -> Unit) {
        val resolvedGuideId = resolveGuideId(guide)
        val normalizedGuide = if (guide.id == resolvedGuideId) guide else guide.copy(id = resolvedGuideId)

        viewModelScope.launch {
            when (val result = guideRepository.updateGuide(normalizedGuide)) {
                is Result.Success -> onSuccess()
                is Result.Failure -> {
                    showError(result.error.toUserMessage())
                    onFailure()
                }
            }
        }
    }

    fun flushPendingChanges() {
        val activeFamilyId = familyId ?: return
        val activeUid = uid ?: return
        viewModelScope.launch {
            guideRepository.syncPendingGuideProgress(
                familyId = activeFamilyId,
                uid = activeUid,
                force = true,
                guideId = guideId,
            )
        }
    }

    private fun persistGuideProgress(guide: Guide, onComplete: (() -> Unit)? = null) {
        val activeUid = uid ?: run { onComplete?.invoke(); return }
        val activeFamilyId = familyId ?: run { onComplete?.invoke(); return }
        viewModelScope.launch {
            val marked = guideRepository.markGuideProgressDirty(guide, activeUid)
            if (marked) {
                guideRepository.syncPendingGuideProgress(
                    familyId = activeFamilyId,
                    uid = activeUid,
                    force = false,
                    guideId = guide.id,
                )
            }
            onComplete?.invoke()
        }
    }

    private fun pointerForStepId(
        guide: Guide,
        stepId: String,
        pieceIndex: Int,
    ): GuideLeafPointer? {
        val normalizedPieceIndex = pieceIndex.coerceAtLeast(0)
        return stepPointerIndex(guide)[pointerIndexKey(stepId, normalizedPieceIndex)]
    }

    private fun stepPointerIndex(guide: Guide): Map<String, GuideLeafPointer> {
        if (pointerCacheGuide === guide) return pointerCacheByStepId

        val rebuiltIndex = mutableMapOf<String, GuideLeafPointer>()
        GuideProgress.buildLeafPointers(guide.sections).forEach { pointer ->
            val stepId = GuideProgress.getStepAtPointer(guide.sections, pointer)?.id
            if (!stepId.isNullOrBlank()) {
                val key = pointerIndexKey(stepId, pointer.sectionPieceIndex)
                if (rebuiltIndex[key] == null) rebuiltIndex[key] = pointer
            }
        }

        pointerCacheGuide = guide
        pointerCacheByStepId = rebuiltIndex
        return rebuiltIndex
    }

    private fun normalizeFetchedGuide(guide: Guide, fallbackGuideId: String): Guide {
        val resolvedId = guide.id.takeIf { it.isNotBlank() } ?: fallbackGuideId
        return if (guide.id == resolvedId) guide else guide.copy(id = resolvedId)
    }

    private fun resolveGuideId(guide: Guide?): String {
        return guide?.id?.takeIf { it.isNotBlank() } ?: guideId
    }

    private fun updateGuideState(guide: Guide) {
        invalidatePointerCache()
        val jumpOptions = buildGuideJumpOptions(guide)
        val currentSelectedJumpOptionKey = currentContentState()?.selectedJumpOptionKey
        val defaultJumpOptionKey = GuideProgress.defaultPointerForGuide(guide)?.let(::guideJumpOptionKey)
        val selectedJumpOptionKey = when {
            currentSelectedJumpOptionKey != null && jumpOptions.any { it.key == currentSelectedJumpOptionKey } -> currentSelectedJumpOptionKey
            defaultJumpOptionKey != null && jumpOptions.any { it.key == defaultJumpOptionKey } -> defaultJumpOptionKey
            else -> jumpOptions.firstOrNull()?.key
        }
        val sectionExpandedState = reconcileSectionExpandedState(
            sections = guide.sections,
            existingState = currentContentState()?.sectionExpandedState.orEmpty(),
        )
        val selectedSectionPieceState = reconcileSelectedSectionPieceState(
            sections = guide.sections,
            existingState = currentContentState()?.selectedSectionPieceState.orEmpty(),
        )
        val canTogglePieceState = buildCanTogglePieceState(guide.sections)
        val currentState = currentContentState()
        _uiState.value = GuideDetailsUiState.Content(
            guide = guide,
            sectionExpandedState = sectionExpandedState,
            selectedSectionPieceState = selectedSectionPieceState,
            canTogglePieceState = canTogglePieceState,
            jumpOptions = jumpOptions,
            selectedJumpOptionKey = selectedJumpOptionKey,
            isUpdatingVisibility = currentState?.isUpdatingVisibility ?: false,
            isStartingGuide = currentState?.isStartingGuide ?: false,
            isDeletingGuide = currentState?.isDeletingGuide ?: false,
            isOwner = guide.ownerUid == uid,
        )
    }

    private fun buildCanTogglePieceState(sections: List<GuideSection>): Map<String, Set<Int>> {
        return buildMap {
            sections.forEachIndexed { sectionIndex, section ->
                val sectionKey = guideSectionKey(section, sectionIndex)
                val pieces = section.pieces.coerceAtLeast(1)
                val activePieceIndex = if (section.completedPieces >= pieces) pieces - 1 else section.completedPieces
                put(sectionKey, setOf(activePieceIndex.coerceIn(0, pieces - 1)))
            }
        }
    }

    private fun completeGuideUntilPointer(
        sections: List<GuideSection>,
        targetPointer: GuideLeafPointer,
    ): List<GuideSection> {
        var updatedSections = sections
        GuideProgress.buildLeafPointers(sections)
            .takeWhile { it != targetPointer }
            .forEach { pointer ->
                updatedSections = GuideProgress.applyLeafCompletion(
                    sections = updatedSections,
                    pointer = pointer,
                    completed = true,
                )
            }
        return updatedSections
    }

    private fun focusPointer(pointer: GuideLeafPointer, guide: Guide) {
        val section = guide.sections.getOrNull(pointer.sectionIndex) ?: return
        val sectionKey = guideSectionKey(section, pointer.sectionIndex)
        updateContentState { state ->
            state.copy(
                sectionExpandedState = state.sectionExpandedState + (sectionKey to true),
                selectedSectionPieceState = state.selectedSectionPieceState + (sectionKey to pointer.sectionPieceIndex),
                selectedJumpOptionKey = guideJumpOptionKey(pointer),
            )
        }
    }

    private fun pointerIndexKey(
        stepId: String,
        pieceIndex: Int,
    ): String = "${pieceIndex.coerceAtLeast(0)}:$stepId"

    private fun invalidatePointerCache() {
        pointerCacheGuide = null
        pointerCacheByStepId = emptyMap()
    }

    private fun updateLoadingState(
        isUpdatingVisibility: Boolean = currentContentState()?.isUpdatingVisibility ?: false,
        isStartingGuide: Boolean = currentContentState()?.isStartingGuide ?: false,
        isDeletingGuide: Boolean = currentContentState()?.isDeletingGuide ?: false,
    ) {
        updateContentState { state ->
            state.copy(
                isUpdatingVisibility = isUpdatingVisibility,
                isStartingGuide = isStartingGuide,
                isDeletingGuide = isDeletingGuide,
            )
        }
    }

    private fun currentContentState(): GuideDetailsUiState.Content? {
        return _uiState.value as? GuideDetailsUiState.Content
    }

    private fun currentGuide(): Guide? {
        return currentContentState()?.guide
    }

    private fun updateContentState(
        transform: (GuideDetailsUiState.Content) -> GuideDetailsUiState.Content,
    ) {
        _uiState.update { state ->
            val contentState = state as? GuideDetailsUiState.Content ?: return@update state
            transform(contentState)
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

    private fun now(): Date = nowProvider()
}
