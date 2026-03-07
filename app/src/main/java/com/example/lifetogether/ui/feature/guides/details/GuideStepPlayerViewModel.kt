package com.example.lifetogether.ui.feature.guides.details

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.ItemResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.logic.GuideLeafPointer
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.logic.GuideRoundGrouping
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.usecase.item.FetchItemByIdUseCase
import com.example.lifetogether.domain.usecase.item.UpdateItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class GuideStepPlayerViewModel @Inject constructor(
    private val fetchItemByIdUseCase: FetchItemByIdUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
) : ViewModel() {
    private companion object {
        const val TAG = "GuideStepPlayerVM"
        const val ALERT_DISMISS_DELAY_MS = 3000L
        const val NAVIGATION_PERSIST_DEBOUNCE_MS = 250L
        const val STEP_PROGRESS_PERSIST_DEBOUNCE_MS = 250L
        const val MIN_REMOTE_PERSIST_INTERVAL_MS = 1500L
    }

    private enum class CompletionMode {
        TOGGLE,
        COMPLETE_IF_NEEDED,
    }

    private var familyId: String? = null
    private var guideId: String? = null
    private var guideJob: Job? = null
    private var dismissAlertJob: Job? = null
    private var navigationPersistJob: Job? = null
    private var stepProgressPersistJob: Job? = null
    private var pendingNavigationGuide: Guide? = null
    private var pendingStepProgressGuide: Guide? = null
    private var lastRemotePersistStartedAtMs: Long = 0L

    private var currentPointerIndex: Int = -1
    private var lastSyncedGuide: Guide? = null
    private var latestPersistRequestVersion: Long = 0L
    private val persistMutex = Mutex()

    private val _uiState = MutableStateFlow(GuideStepPlayerUiState())
    val uiState: StateFlow<GuideStepPlayerUiState> = _uiState.asStateFlow()

    fun dismissAlert() {
        dismissAlertJob?.cancel()
        dismissAlertJob = viewModelScope.launch {
            delay(ALERT_DISMISS_DELAY_MS)
            clearError()
        }
    }

    fun setUpGuide(
        familyId: String,
        guideId: String,
    ) {
        val isNewGuideContext = this.familyId != familyId || this.guideId != guideId
        Log.d(
            TAG,
            "setUpGuide familyId=$familyId guideId=$guideId isNewGuideContext=$isNewGuideContext activeJob=${guideJob?.isActive == true}",
        )
        if (!isNewGuideContext && guideJob?.isActive == true) {
            Log.d(TAG, "setUpGuide skipped because existing observer is active for same context")
            return
        }

        if (isNewGuideContext) {
            currentPointerIndex = -1
            lastSyncedGuide = null
            cancelPendingNavigationPersistence()
            cancelPendingStepProgressPersistence()
            Log.d(TAG, "setUpGuide reset pointer and cached synced guide for new context")
        }

        this.familyId = familyId
        this.guideId = guideId

        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            Log.d(TAG, "setUpGuide subscribing to local guide flow for guideId=$guideId")
            fetchItemByIdUseCase(
                familyId = familyId,
                id = guideId,
                listName = Constants.GUIDES_TABLE,
                itemType = Guide::class,
            ).collect { result ->
                when (result) {
                    is ItemResultListener.Success -> {
                        val guide = result.item as? Guide ?: run {
                            Log.w(
                                TAG,
                                "Observed non-guide item from fetchItemByIdUseCase: ${result.item::class.simpleName}",
                            )
                            return@collect
                        }
                        Log.d(
                            TAG,
                            "Observed guide emission id=${guide.id} started=${guide.started} resume=${guide.resume} lastUpdated=${guide.lastUpdated.time}",
                        )
                        lastSyncedGuide = guide
                        applyGuideUpdate(
                            uiGuide = guide,
                            forcePointerRecompute = currentPointerIndex < 0,
                        )
                    }

                    is ItemResultListener.Failure -> {
                        Log.e(TAG, "Guide observation failed: ${result.message}")
                        showError(result.message)
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
        applyGuideUpdate(
            uiGuide = currentGuide,
            forcePointerRecompute = false,
        )
        scheduleNavigationPersistence(currentGuide)
    }

    fun toggleCurrentStepCompletion() {
        updateCurrentStep(
            completionMode = CompletionMode.TOGGLE,
            moveToNext = false,
        )
    }

    fun completeCurrentAndGoNext() {
        updateCurrentStep(
            completionMode = CompletionMode.COMPLETE_IF_NEEDED,
            moveToNext = true,
        )
    }

    private fun updateCurrentStep(
        completionMode: CompletionMode,
        moveToNext: Boolean,
    ) {
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

        cancelPendingNavigationPersistence()
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
        updateUiState(
            guide = uiGuide,
            forcePointerRecompute = forcePointerRecompute,
        )
        persistedGuide?.let(::scheduleStepProgressPersistence)
    }

    private fun scheduleNavigationPersistence(guide: Guide) {
        cancelPendingNavigationPersistence()
        cancelPendingStepProgressPersistence()
        pendingNavigationGuide = guide
        Log.d(
            TAG,
            "scheduleNavigationPersistence guideId=${guide.id} pointerIndex=$currentPointerIndex debounceMs=$NAVIGATION_PERSIST_DEBOUNCE_MS",
        )
        navigationPersistJob = viewModelScope.launch {
            delay(NAVIGATION_PERSIST_DEBOUNCE_MS)
            val pointerResumeGuide = buildGuideForPersistence(
                guide = guide,
                sections = guide.sections,
                resumePointer = currentPointer(guide),
            )
            Log.d(
                TAG,
                "scheduleNavigationPersistence firing persist guideId=${pointerResumeGuide.id} resume=${pointerResumeGuide.resume}",
            )
            persistGuide(pointerResumeGuide)
        }
    }

    private fun cancelPendingNavigationPersistence() {
        navigationPersistJob?.cancel()
        navigationPersistJob = null
        pendingNavigationGuide = null
    }

    private fun scheduleStepProgressPersistence(guide: Guide) {
        cancelPendingStepProgressPersistence()
        pendingStepProgressGuide = guide
        Log.d(
            TAG,
            "scheduleStepProgressPersistence cached guideId=${guide.id} debounceMs=$STEP_PROGRESS_PERSIST_DEBOUNCE_MS",
        )
        stepProgressPersistJob = viewModelScope.launch {
            delay(STEP_PROGRESS_PERSIST_DEBOUNCE_MS)
            flushPendingStepProgressPersistence()
        }
    }

    private fun flushPendingStepProgressPersistence() {
        val latestGuide = pendingStepProgressGuide ?: return
        val now = System.currentTimeMillis()
        val elapsed = now - lastRemotePersistStartedAtMs
        if (elapsed < MIN_REMOTE_PERSIST_INTERVAL_MS) {
            val waitMs = MIN_REMOTE_PERSIST_INTERVAL_MS - elapsed
            Log.d(
                TAG,
                "scheduleStepProgressPersistence throttled guideId=${latestGuide.id} waitMs=$waitMs minIntervalMs=$MIN_REMOTE_PERSIST_INTERVAL_MS",
            )
            stepProgressPersistJob = viewModelScope.launch {
                delay(waitMs)
                flushPendingStepProgressPersistence()
            }
            return
        }

        pendingStepProgressGuide = null
        lastRemotePersistStartedAtMs = now
        Log.d(
            TAG,
            "scheduleStepProgressPersistence firing latest cached persist guideId=${latestGuide.id} resume=${latestGuide.resume}",
        )
        persistGuide(latestGuide)
    }

    private fun cancelPendingStepProgressPersistence() {
        stepProgressPersistJob?.cancel()
        stepProgressPersistJob = null
        pendingStepProgressGuide = null
    }

    fun flushPendingChanges() {
        val latestStepGuide = pendingStepProgressGuide
        if (latestStepGuide != null) {
            Log.d(TAG, "flushPendingChanges persisting cached step progress guideId=${latestStepGuide.id}")
            cancelPendingStepProgressPersistence()
            persistGuide(latestStepGuide)
            return
        }

        val latestNavigationGuide = pendingNavigationGuide
        if (latestNavigationGuide != null) {
            Log.d(TAG, "flushPendingChanges persisting cached navigation guideId=${latestNavigationGuide.id}")
            cancelPendingNavigationPersistence()
            val pointerResumeGuide = buildGuideForPersistence(
                guide = latestNavigationGuide,
                sections = latestNavigationGuide.sections,
                resumePointer = currentPointer(latestNavigationGuide),
            )
            persistGuide(pointerResumeGuide)
        }
    }

    private fun persistGuide(guide: Guide) {
        val resolvedGuideId = resolveGuideId(guide) ?: run {
            Log.e(TAG, "persistGuide failed: missing guide id in guide=${guide.id} cachedGuideId=$guideId")
            showError("Unable to save guide progress. Missing guide id.")
            return
        }
        val normalizedGuide = if (guide.id == resolvedGuideId) {
            guide
        } else {
            guide.copy(id = resolvedGuideId)
        }
        val requestVersion = ++latestPersistRequestVersion
        Log.d(
            TAG,
            "persistGuide queued requestVersion=$requestVersion guideId=${normalizedGuide.id} started=${normalizedGuide.started} resume=${normalizedGuide.resume} lastUpdated=${normalizedGuide.lastUpdated.time}",
        )

        viewModelScope.launch {
            persistMutex.withLock {
                if (requestVersion < latestPersistRequestVersion) {
                    Log.d(
                        TAG,
                        "persistGuide skipping stale requestVersion=$requestVersion latestRequestVersion=$latestPersistRequestVersion guideId=${normalizedGuide.id}",
                    )
                    return@withLock
                }
                Log.d(
                    TAG,
                    "persistGuide uploading requestVersion=$requestVersion guideId=${normalizedGuide.id}",
                )
                val result = updateItemUseCase(normalizedGuide, Constants.GUIDES_TABLE)

                when (result) {
                    is ResultListener.Success -> {
                        Log.d(
                            TAG,
                            "persistGuide upload success requestVersion=$requestVersion guideId=${normalizedGuide.id}",
                        )
                        lastSyncedGuide = normalizedGuide
                    }

                    is ResultListener.Failure -> {
                        Log.e(
                            TAG,
                            "persistGuide upload failure requestVersion=$requestVersion latestRequestVersion=$latestPersistRequestVersion message=${result.message}",
                        )
                        if (requestVersion == latestPersistRequestVersion) {
                            val fallbackGuide = lastSyncedGuide
                            if (fallbackGuide != null) {
                                Log.d(
                                    TAG,
                                    "persistGuide restoring fallback guideId=${fallbackGuide.id} after failure",
                                )
                                applyGuideUpdate(
                                    uiGuide = fallbackGuide,
                                    forcePointerRecompute = true,
                                )
                            }
                        }
                        showError(result.message)
                    }
                }
            }
        }
    }

    private fun resolveGuideId(guide: Guide): String? {
        return guide.id?.takeIf { it.isNotBlank() }
            ?: guideId?.takeIf { it.isNotBlank() }
    }

    private fun showError(message: String) {
        Log.e(TAG, "showError: $message")
        dismissAlertJob?.cancel()
        _uiState.update { state ->
            state.copy(
                showAlertDialog = true,
                error = message,
            )
        }
    }

    private fun clearError() {
        _uiState.update { state ->
            state.copy(
                showAlertDialog = false,
                error = "",
            )
        }
    }

    private fun updateUiState(
        guide: Guide,
        forcePointerRecompute: Boolean,
    ) {
        val leaves = GuideProgress.buildLeafPointers(guide.sections)
        if (leaves.isEmpty()) {
            currentPointerIndex = -1
            _uiState.value = mergeTransientUiState(
                GuideStepPlayerUiState(guide = guide),
            )
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
        val sectionProgress = currentSection?.let { GuideProgress.sectionProgress(it) } ?: (0 to 0)
        val sectionAmountProgressText = buildSectionAmountProgressText(
            section = currentSection,
            pointer = pointer,
        )
        val roundContext = resolveRoundContext(
            section = currentSection,
            pointer = pointer,
        )

        return GuideStepPlayerUiState(
            guide = guide,
            currentStep = currentStep,
            nextStep = nextStep,
            currentStepCompleted = GuideProgress.isPointerCompleted(guide.sections, pointer),
            canToggleCurrentStep = GuideProgress.canTogglePointer(guide.sections, pointer),
            currentRoundGroupLabel = roundContext?.label.orEmpty(),
            currentRoundGroupMeta = roundContext?.meta.orEmpty(),
            currentStepNumber = currentPointerIndex + 1,
            totalSteps = leaves.size,
            sectionTitle = currentSection?.title.orEmpty(),
            sectionAmountProgressText = sectionAmountProgressText,
            sectionProgressPercent = currentSection?.let { GuideProgress.progressPercent(it) } ?: 0,
            sectionProgressText = "${sectionProgress.first} / ${sectionProgress.second}",
            canGoPrevious = currentPointerIndex > 0,
            canGoNext = currentPointerIndex < leaves.lastIndex,
        )
    }

    private fun buildSectionAmountProgressText(
        section: GuideSection?,
        pointer: GuideLeafPointer,
    ): String {
        if (section == null || section.amount <= 1) return ""
        return "Part ${pointer.sectionAmountIndex + 1}/${section.amount}"
    }

    private fun mergeTransientUiState(baseState: GuideStepPlayerUiState): GuideStepPlayerUiState {
        val currentState = _uiState.value
        return baseState.copy(
            showAlertDialog = currentState.showAlertDialog,
            error = currentState.error,
        )
    }

    private fun resolveRoundContext(
        section: GuideSection?,
        pointer: GuideLeafPointer,
    ): RoundDisplayContext? {
        val activeSection = section ?: return null
        val (containerSteps, indexInContainer) = if (pointer.subStepIndex != null) {
            val parentStep = activeSection.steps.getOrNull(pointer.stepIndex) ?: return null
            parentStep.subSteps to pointer.subStepIndex
        } else {
            activeSection.steps to pointer.stepIndex
        }

        val roundGroup = GuideRoundGrouping.findRoundGroupContext(containerSteps, indexInContainer)
            ?: return null

        val groupLabel = GuideRoundGrouping.formatRoundLabel(roundGroup.range)
        if (roundGroup.size <= 1) {
            return RoundDisplayContext(
                label = groupLabel,
                meta = "",
            )
        }

        val currentStep = containerSteps.getOrNull(indexInContainer) ?: return null
        val currentRoundNumber = GuideRoundGrouping.parseRoundNumber(
            currentStep.name.ifBlank { currentStep.title },
        ) ?: roundGroup.range.first

        val position = (currentRoundNumber - roundGroup.range.first) + 1
        return RoundDisplayContext(
            label = groupLabel,
            meta = "Current: R$currentRoundNumber ($position/${roundGroup.size})",
        )
    }

    private data class RoundDisplayContext(
        val label: String,
        val meta: String,
    )
}
