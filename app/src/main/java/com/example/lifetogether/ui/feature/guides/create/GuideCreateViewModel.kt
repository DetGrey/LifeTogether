package com.example.lifetogether.ui.feature.guides.create

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.logic.GuideProgress
import com.example.lifetogether.domain.logic.GuideRoundGrouping
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.domain.model.guides.GuideVisibility
import com.example.lifetogether.domain.usecase.item.SaveItemUseCase
import com.example.lifetogether.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GuideCreateViewModel @Inject constructor(
    private val saveItemUseCase: SaveItemUseCase,
) : ViewModel() {
    private var familyId: String? = null
    private var uid: String? = null

    var title: String by mutableStateOf("")
    var description: String by mutableStateOf("")
    var visibility: GuideVisibility by mutableStateOf(GuideVisibility.PRIVATE)

    private val _sections = MutableStateFlow<List<GuideSection>>(emptyList())
    val sections: StateFlow<List<GuideSection>> = _sections.asStateFlow()

    var showAlertDialog: Boolean by mutableStateOf(false)
    var error: String by mutableStateOf("")

    fun dismissAlert() {
        viewModelScope.launch {
            delay(3000)
            showAlertDialog = false
            error = ""
        }
    }

    fun setContext(
        familyId: String,
        uid: String,
    ) {
        this.familyId = familyId
        this.uid = uid
    }

    fun addSection(
        sectionTitle: String,
        amount: Int = 1,
    ) {
        val normalizedTitle = sectionTitle.trim().ifBlank {
            "Section ${_sections.value.size + 1}"
        }
        val normalizedAmount = amount.coerceAtLeast(1)

        _sections.value = _sections.value + GuideSection(
            id = UUID.randomUUID().toString(),
            orderNumber = _sections.value.size + 1,
            title = normalizedTitle,
            amount = normalizedAmount,
            steps = emptyList(),
        )
    }

    fun addStep(
        sectionId: String,
        content: String,
        type: GuideStepType = GuideStepType.NUMBERED,
    ) {
        val normalizedContent = content.trim()
        if (normalizedContent.isBlank()) return

        _sections.value = _sections.value.map { section ->
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

                section.copy(
                    steps = section.steps + newSteps,
                )
            }
        }
    }

    private fun expandRoundDraft(draft: String): List<GuideStep> {
        val parsedPrefix = GuideRoundGrouping.parseRoundPrefix(draft)
        if (parsedPrefix == null) {
            return listOf(
                GuideStep(
                    id = UUID.randomUUID().toString(),
                    type = GuideStepType.ROUND,
                    content = draft,
                ),
            )
        }

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

    fun saveGuide(onSuccess: (guideId: String) -> Unit) {
        val activeFamilyId = familyId
        val activeUid = uid

        if (activeFamilyId.isNullOrBlank() || activeUid.isNullOrBlank()) {
            error = "Please connect to a family first"
            showAlertDialog = true
            return
        }

        if (title.trim().isEmpty()) {
            error = "Please enter a title"
            showAlertDialog = true
            return
        }

        val normalizedSections = _sections.value
            .mapIndexed { index, section ->
                GuideProgress.updateSectionCompletion(
                    section.copy(orderNumber = index + 1),
                )
            }

        val guide = Guide(
            familyId = activeFamilyId,
            itemName = title.trim(),
            description = description.trim(),
            lastUpdated = Date(),
            visibility = visibility,
            ownerUid = activeUid,
            started = false,
            sections = normalizedSections,
            resume = null,
        )

        viewModelScope.launch {
            when (val result = saveItemUseCase.invoke(guide, Constants.GUIDES_TABLE)) {
                is StringResultListener.Success -> {
                    onSuccess(result.string)
                }

                is StringResultListener.Failure -> {
                    error = result.message
                    showAlertDialog = true
                }
            }
        }
    }
}
