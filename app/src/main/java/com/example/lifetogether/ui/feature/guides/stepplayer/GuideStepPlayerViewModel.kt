package com.example.lifetogether.ui.feature.guides.stepplayer

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.GuideLeafPointer
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class GuideStepPlayerViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val guideRepository: GuideRepository,
) : ViewModel() {
    private companion object {
        const val TAG = "GuideStepPlayerVM"
        const val ALERT_DISMISS_DELAY_MS = 3000L
        const val GUIDE_PERSIST_DEBOUNCE_MS = 250L
    }

    private enum class CompletionMode {
        TOGGLE,
        COMPLETE_IF_NEEDED,
    }

    private var familyId: String? = null
    private var uid: String? = null
    private var guideId: String? = null
    private var guideJob: Job? = null
    private var dismissAlertJob: Job? = null
    private var guidePersistJob: Job? = null
    private var pendingGuide: Guide? = null

    private var currentPointerIndex: Int = -1

    private val _uiState = MutableStateFlow(GuideStepPlayerUiState())
    val uiState: StateFlow<GuideStepPlayerUiState> = _uiState.asStateFlow()

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
                    guideId?.let { startGuideJob(it) }
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    uid = null
                }
            }
        }
    }

    fun dismissAlert() {
        dismissAlertJob?.cancel()
        dismissAlertJob = viewModelScope.launch {
            delay(ALERT_DISMISS_DELAY_MS)
            clearError()
        }
    }

    fun setUp(guideId: String) {
        val isNewGuide = this.guideId != guideId
        if (isNewGuide) {
            this.guideId = guideId
            currentPointerIndex = -1
            cancelPendingGuidePersistence()
        }
        if (familyId != null && uid != null) {
            startGuideJob(guideId)
        }
    }

    private fun startGuideJob(guideId: String) {
        val familyIdValue = familyId ?: return
        val uidValue = uid ?: return

        if (this.guideId == guideId && guideJob?.isActive == true) {
            Log.d(TAG, "startGuideJob skipped: active job for same context")
            return
        }

        Log.d(TAG, "startGuideJob familyId=$familyIdValue guideId=$guideId")
        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            Log.d(TAG, "startGuideJob subscribing to local guide flow for guideId=$guideId")
            guideRepository.observeGuideById(
                familyId = familyIdValue,
                id = guideId,
                uid = uidValue,
            ).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val guide = result.data
                        Log.d(
                            TAG,
                            "Observed guide emission id=${guide.id} started=${guide.started} resume=${guide.resume} lastUpdated=${guide.lastUpdated.time}",
                        )
                        applyGuideUpdate(
                            uiGuide = guide,
                            forcePointerRecompute = currentPointerIndex < 0,
                        )
                    }
                    is Result.Failure -> {
                        Log.e(TAG, "Guide observation failed: ${result.error}")
                        showError(result.error)
                    }
                }
            }
        }
    }

    fun goToPreviousStep() {
        val currentGuide = _uiState.value.guide ?: return
        val leaves = GuideProgress.buildLeafPointers(currentGuide.sections)
        if (leaves.isEmpty()) return

        val targetIndex = currentPointerIndex - 1
        if (targetIndex !in leaves.indices) return

        currentPointerIndex = targetIndex
        applyGuideUpdate(uiGuide = currentGuide, forcePointerRecompute = false)
        scheduleNavigationPersistence(currentGuide)
    }

    fun toggleCurrentStepCompletion() {
        updateCurrentStep(completionMode = CompletionMode.TOGGLE, moveToNext = false)
    }

    fun completeCurrentAndGoNext() {
        updateCurrentStep(completionMode = CompletionMode.COMPLETE_IF_NEEDED, moveToNext = true)
    }

    private fun updateCurrentStep(completionMode: CompletionMode, moveToNext: Boolean) {
        val currentGuide = _uiState.value.guide ?: return
        val pointer = currentPointer(currentGuide) ?: return

        val canToggleCurrent = GuideProgress.canTogglePointer(currentGuide.sections, pointer)
        val isCurrentCompleted = GuideProgress.isPointerCompleted(currentGuide.sections, pointer)
        if (!canToggleCurrent && !moveToNext) return

        val shouldApplyCompletion = when (completionMode) {
            CompletionMode.TOGGLE -> canToggleCurrent
            CompletionMode.COMPLETE_IF_NEEDED -> canToggleCurrent && !isCurrentCompleted
        }

        val completionValue = when (completionMode) {
            CompletionMode.TOGGLE -> !isCurrentCompleted
            CompletionMode.COMPLETE_IF_NEEDED -> true
        }
        val updatedSections = if (shouldApplyCompletion) {
            GuideProgress.applyLeafCompletion(
                sections = currentGuide.sections,
                pointer = pointer,
                completed = completionValue,
            )
        } else {
            currentGuide.sections
        }

        val hasMovedToNext = if (moveToNext) movePointerToNextLeaf(updatedSections) else false
        if (!shouldApplyCompletion && !hasMovedToNext) return

        val updatedGuide = buildGuideForPersistence(
            guide = currentGuide,
            sections = updatedSections,
            resumePointer = GuideProgress.firstIncompletePointer(updatedSections),
        )
        applyGuideUpdate(
            uiGuide = updatedGuide,
            forcePointerRecompute = false,
            persistedGuide = updatedGuide,
        )
    }

    private fun currentPointer(guide: Guide): GuideLeafPointer? {
        val leaves = GuideProgress.buildLeafPointers(guide.sections)
        if (leaves.isEmpty() || currentPointerIndex !in leaves.indices) return null
        return leaves[currentPointerIndex]
    }

    private fun movePointerToNextLeaf(sections: List<GuideSection>): Boolean {
        val leaves = GuideProgress.buildLeafPointers(sections)
        if (leaves.isEmpty()) {
            currentPointerIndex = -1
            return true
        }
        if (currentPointerIndex < leaves.lastIndex) {
            currentPointerIndex += 1
            return true
        }
        return false
    }

    private fun buildGuideForPersistence(
        guide: Guide,
        sections: List<GuideSection> = guide.sections,
        resumePointer: GuideLeafPointer? = null,
    ): Guide {
        val resolvedResumePointer = resumePointer ?: currentPointer(guide)
        return guide.copy(
            started = true,
            sections = sections,
            resume = resolvedResumePointer?.let(GuideProgress::resumeFromPointer),
            lastUpdated = Date(),
        )
    }

    private fun applyGuideUpdate(
        uiGuide: Guide,
        forcePointerRecompute: Boolean = false,
        persistedGuide: Guide? = null,
    ) {
        updateUiState(guide = uiGuide, forcePointerRecompute = forcePointerRecompute)
        persistedGuide?.let(::scheduleStepProgressPersistence)
    }

    private fun scheduleNavigationPersistence(guide: Guide) {
        val pointerResumeGuide = buildGuideForPersistence(
            guide = guide,
            sections = guide.sections,
            resumePointer = currentPointer(guide),
        )
        scheduleGuidePersistence(pointerResumeGuide)
    }

    private fun scheduleStepProgressPersistence(guide: Guide) { scheduleGuidePersistence(guide) }

    private fun scheduleGuidePersistence(guide: Guide) {
        val normalizedGuide = normalizeGuideForPersistence(guide) ?: return
        pendingGuide = normalizedGuide
        guidePersistJob?.cancel()
        Log.d(TAG, "scheduleGuidePersistence cached guideId=${normalizedGuide.id} debounceMs=$GUIDE_PERSIST_DEBOUNCE_MS")
        guidePersistJob = viewModelScope.launch {
            delay(GUIDE_PERSIST_DEBOUNCE_MS)
            flushPendingGuidePersistence()
        }
    }

    fun flushPendingChanges() {
        flushPendingGuidePersistence(immediate = true)
        val activeFamilyId = familyId
        val activeUid = uid
        val activeGuideId = guideId
        if (!activeFamilyId.isNullOrBlank() && !activeUid.isNullOrBlank()) {
            viewModelScope.launch {
                guideRepository.syncPendingGuideProgress(
                    familyId = activeFamilyId,
                    uid = activeUid,
                    force = true,
                    guideId = activeGuideId,
                )
            }
        }
    }

    private fun flushPendingGuidePersistence(immediate: Boolean = false) {
        val latestGuide = pendingGuide ?: return
        if (immediate) {
            guidePersistJob?.cancel()
            guidePersistJob = null
        }
        pendingGuide = null
        Log.d(TAG, "flushPendingGuidePersistence persisting cached guideId=${latestGuide.id}")
        persistGuide(latestGuide, forceUpload = immediate)
    }

    private fun cancelPendingGuidePersistence() {
        guidePersistJob?.cancel()
        guidePersistJob = null
        pendingGuide = null
    }

    private fun persistGuide(guide: Guide, forceUpload: Boolean = false) {
        val normalizedGuide = normalizeGuideForPersistence(guide) ?: return
        val activeUid = uid ?: return
        val activeFamilyId = familyId ?: return
        Log.d(TAG, "persistGuide local save guideId=${normalizedGuide.id} started=${normalizedGuide.started} resume=${normalizedGuide.resume} lastUpdated=${normalizedGuide.lastUpdated.time} forceUpload=$forceUpload")

        viewModelScope.launch {
            val marked = guideRepository.markGuideProgressDirty(normalizedGuide, activeUid)
            if (!marked) {
                showError("Unable to save guide progress. Missing guide id.")
                return@launch
            }
            guideRepository.syncPendingGuideProgress(
                familyId = activeFamilyId,
                uid = activeUid,
                force = forceUpload,
                guideId = normalizedGuide.id,
            )
        }
    }

    private fun normalizeGuideForPersistence(guide: Guide): Guide? {
        val resolvedGuideId = resolveGuideId(guide) ?: run {
            Log.e(TAG, "persistGuide failed: missing guide id in guide=${guide.id} cachedGuideId=$guideId")
            showError("Unable to save guide progress. Missing guide id.")
            return null
        }
        return if (guide.id == resolvedGuideId) guide else guide.copy(id = resolvedGuideId)
    }

    private fun resolveGuideId(guide: Guide): String? {
        return guide.id?.takeIf { it.isNotBlank() } ?: guideId?.takeIf { it.isNotBlank() }
    }

    private fun showError(message: String) {
        Log.e(TAG, "showError: $message")
        dismissAlertJob?.cancel()
        _uiState.update { state ->
            state.copy(showAlertDialog = true, error = message)
        }
    }

    private fun clearError() {
        _uiState.update { state ->
            state.copy(showAlertDialog = false, error = "")
        }
    }

    private fun updateUiState(guide: Guide, forcePointerRecompute: Boolean) {
        val leaves = GuideProgress.buildLeafPointers(guide.sections)
        if (leaves.isEmpty()) {
            currentPointerIndex = -1
            _uiState.value = mergeTransientUiState(GuideStepPlayerUiState(guide = guide))
            return
        }

        ensureValidPointerIndex(
            guide = guide,
            leaves = leaves,
            forcePointerRecompute = forcePointerRecompute,
        )

        val pointer = leaves[currentPointerIndex]
        _uiState.value = mergeTransientUiState(
            buildUiState(
                guide = guide,
                leaves = leaves,
                pointer = pointer,
            ),
        )
    }

    private fun ensureValidPointerIndex(
        guide: Guide,
        leaves: List<GuideLeafPointer>,
        forcePointerRecompute: Boolean,
    ) {
        if (!forcePointerRecompute && currentPointerIndex in leaves.indices) return

        val defaultPointer = GuideProgress.defaultPointerForGuide(guide) ?: leaves.first()
        val defaultPointerIndex = leaves.indexOf(defaultPointer)
        currentPointerIndex = if (defaultPointerIndex == -1) 0 else defaultPointerIndex
    }

    private fun buildUiState(
        guide: Guide,
        leaves: List<GuideLeafPointer>,
        pointer: GuideLeafPointer,
    ): GuideStepPlayerUiState {
        val currentStep = GuideProgress.getStepAtPointer(guide.sections, pointer)
        val nextStep = leaves.getOrNull(currentPointerIndex + 1)
            ?.let { GuideProgress.getStepAtPointer(guide.sections, it) }

        val currentSection = guide.sections.getOrNull(pointer.sectionIndex)
        val currentPartProgress = currentSection?.let {
            GuideProgress.sectionAmountProgress(section = it, amountIndex = pointer.sectionAmountIndex)
        } ?: (0 to 0)
        val currentPartLabel = buildCurrentPartLabel(section = currentSection, pointer = pointer)
        return GuideStepPlayerUiState(
            guide = guide,
            currentStep = currentStep,
            nextStep = nextStep,
            currentStepCompleted = GuideProgress.isPointerCompleted(guide.sections, pointer),
            canToggleCurrentStep = GuideProgress.canTogglePointer(guide.sections, pointer),
            currentRoundGroupLabel = "",
            currentRoundGroupMeta = "",
            currentStepNumber = currentPointerIndex + 1,
            totalSteps = leaves.size,
            sectionTitle = currentSection?.title.orEmpty(),
            sectionSubtitle = currentSection?.subtitle.orEmpty(),
            currentPartLabel = currentPartLabel,
            currentPartProgressPercent = currentSection?.let {
                GuideProgress.sectionAmountProgressPercent(
                    section = it,
                    amountIndex = pointer.sectionAmountIndex,
                )
            } ?: 0,
            currentPartProgressText = "${currentPartProgress.first} / ${currentPartProgress.second}",
            canGoPrevious = currentPointerIndex > 0,
            canGoNext = currentPointerIndex < leaves.lastIndex,
        )
    }

    private fun buildCurrentPartLabel(section: GuideSection?, pointer: GuideLeafPointer): String {
        if (section == null || section.amount <= 1) return ""
        return "Part ${pointer.sectionAmountIndex + 1}/${section.amount}"
    }

    private fun mergeTransientUiState(baseState: GuideStepPlayerUiState): GuideStepPlayerUiState {
        val currentState = _uiState.value
        return baseState.copy(showAlertDialog = currentState.showAlertDialog, error = currentState.error)
    }
}
