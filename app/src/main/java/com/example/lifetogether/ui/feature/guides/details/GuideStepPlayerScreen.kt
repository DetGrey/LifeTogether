package com.example.lifetogether.ui.feature.guides.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Icon
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType
import com.example.lifetogether.ui.common.TopBar
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.FirebaseViewModel

@Composable
fun GuideStepPlayerRoute(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    guideId: String,
    viewModelStoreOwner: ViewModelStoreOwner? = null,
) {
    val guideStepPlayerViewModel: GuideStepPlayerViewModel = if (viewModelStoreOwner != null) {
        hiltViewModel(viewModelStoreOwner)
    } else {
        hiltViewModel()
    }

    GuideStepPlayerScreen(
        appNavigator = appNavigator,
        firebaseViewModel = firebaseViewModel,
        guideId = guideId,
        guideStepPlayerViewModel = guideStepPlayerViewModel,
    )
}

@Composable
fun GuideStepPlayerScreen(
    appNavigator: AppNavigator? = null,
    firebaseViewModel: FirebaseViewModel? = null,
    guideId: String,
    guideStepPlayerViewModel: GuideStepPlayerViewModel,
) {
    val userInformation by firebaseViewModel?.userInformation!!.collectAsState()
    val uiState by guideStepPlayerViewModel.uiState.collectAsState()

    BackHandler(enabled = appNavigator != null) {
        guideStepPlayerViewModel.flushPendingChanges()
        appNavigator?.navigateBack()
    }

    LaunchedEffect(userInformation?.familyId, guideId) {
        val familyId = userInformation?.familyId
        if (!familyId.isNullOrBlank()) {
            guideStepPlayerViewModel.setUpGuide(familyId, guideId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            guideStepPlayerViewModel.flushPendingChanges()
        }
    }

    val canPrimaryAction = uiState.currentStep != null && (uiState.canGoNext || uiState.canToggleCurrentStep)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TopBar(
                leftIcon = Icon(
                    resId = R.drawable.ic_back_arrow,
                    description = "back arrow icon",
                ),
                onLeftClick = {
                    guideStepPlayerViewModel.flushPendingChanges()
                    appNavigator?.navigateBack()
                },
                text = "Step player",
            )
        }

        item {
            StepPlayerOverviewCard(uiState)
        }

        item {
            GuideStepCard(
                header = "Current step",
                step = uiState.currentStep,
                stepNumber = uiState.currentStepNumber,
                roundGroupLabel = uiState.currentRoundGroupLabel,
                roundGroupMeta = uiState.currentRoundGroupMeta,
                emphasized = true,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canGoPrevious,
                    onClick = { guideStepPlayerViewModel.goToPreviousStep() },
                ) {
                    Text("Previous")
                }

                Button(
                    modifier = Modifier.weight(1.35f),
                    enabled = canPrimaryAction,
                    onClick = { guideStepPlayerViewModel.completeCurrentAndGoNext() },
                ) {
                    Text(
                        text = when {
                            uiState.currentStepCompleted && uiState.canGoNext -> "Next step"
                            uiState.currentStepCompleted -> "Completed"
                            uiState.canGoNext -> "Complete + next"
                            else -> "Complete step"
                        },
                    )
                }
            }
        }

        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.currentStep != null && uiState.canToggleCurrentStep,
                onClick = { guideStepPlayerViewModel.toggleCurrentStepCompletion() },
            ) {
                Text(
                    text = if (uiState.currentStepCompleted) {
                        "Mark current step incomplete"
                    } else {
                        "Mark current step complete"
                    },
                )
            }
        }

        if (uiState.nextStep != null) {
            item {
                GuideStepCard(
                    header = "Up next",
                    step = uiState.nextStep,
                    stepNumber = (uiState.currentStepNumber + 1).coerceAtMost(uiState.totalSteps),
                    emphasized = false,
                )
            }
        }
    }

    if (uiState.showAlertDialog) {
        ErrorAlertDialog(uiState.error)
        guideStepPlayerViewModel.dismissAlert()
    }
}

@Composable
private fun StepPlayerOverviewCard(uiState: GuideStepPlayerUiState) {
    val sectionName = uiState.sectionTitle.ifBlank { "Guide section" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = sectionName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (uiState.sectionAmountProgressText.isNotBlank()) {
                Text(
                    text = uiState.sectionAmountProgressText,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = { uiState.sectionProgressPercent / 100f },
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            )

            Text(
                text = "Section progress: ${uiState.sectionProgressText} (${uiState.sectionProgressPercent}%)",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Step ${uiState.currentStepNumber} of ${uiState.totalSteps}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun GuideStepCard(
    header: String,
    step: GuideStep?,
    stepNumber: Int?,
    roundGroupLabel: String = "",
    roundGroupMeta: String = "",
    emphasized: Boolean,
) {
    val cardColor = if (emphasized) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (emphasized) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = header,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
            if (step == null) {
                Text(
                    text = "No step available",
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                GuideStepCardBody(
                    step = step,
                    stepNumber = stepNumber,
                    textColor = textColor,
                    roundGroupLabel = roundGroupLabel,
                    roundGroupMeta = roundGroupMeta,
                )
            }
        }
    }
}

@Composable
private fun GuideStepCardBody(
    step: GuideStep,
    stepNumber: Int?,
    textColor: Color,
    roundGroupLabel: String,
    roundGroupMeta: String,
) {
    when (step.type) {
        GuideStepType.ROUND -> {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(roundGroupLabel.ifBlank { roundLabel(step) })
                    }
                    if (step.content.isNotBlank()) {
                        append(" ")
                        append(step.content)
                    }
                },
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
            )
            if (roundGroupMeta.isNotBlank()) {
                Text(
                    text = roundGroupMeta,
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        GuideStepType.COMMENT, GuideStepType.UNKNOWN -> {
            Text(
                text = commentText(step),
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        GuideStepType.NUMBERED -> {
            Text(
                text = "${stepNumber ?: 1}. ${numberedText(step)}",
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        GuideStepType.SUBSECTION -> {
            Text(
                text = subsectionLabel(step),
                color = textColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (step.subSteps.isNotEmpty()) {
                GuideSubStepList(
                    steps = step.subSteps,
                    textColor = textColor,
                    indentLevel = 1,
                )
            } else if (step.content.isNotBlank()) {
                Text(
                    text = step.content,
                    modifier = Modifier.padding(start = 14.dp),
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun GuideSubStepList(
    steps: List<GuideStep>,
    textColor: Color,
    indentLevel: Int,
) {
    var numberedIndex = 1
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        steps.forEach { step ->
            when (step.type) {
                GuideStepType.ROUND -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(roundLabel(step))
                            }
                            if (step.content.isNotBlank()) {
                                append(" ")
                                append(step.content)
                            }
                        },
                        modifier = Modifier.padding(start = (indentLevel * 14).dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                GuideStepType.COMMENT, GuideStepType.UNKNOWN -> {
                    Text(
                        text = commentText(step),
                        modifier = Modifier.padding(start = (indentLevel * 14).dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                GuideStepType.NUMBERED -> {
                    Text(
                        text = "$numberedIndex. ${numberedText(step)}",
                        modifier = Modifier.padding(start = (indentLevel * 14).dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    numberedIndex += 1
                }

                GuideStepType.SUBSECTION -> {
                    Text(
                        text = subsectionLabel(step),
                        modifier = Modifier.padding(start = (indentLevel * 14).dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                    if (step.subSteps.isNotEmpty()) {
                        GuideSubStepList(
                            steps = step.subSteps,
                            textColor = textColor,
                            indentLevel = indentLevel + 1,
                        )
                    } else if (step.content.isNotBlank()) {
                        Text(
                            text = step.content,
                            modifier = Modifier.padding(start = ((indentLevel + 1) * 14).dp),
                            color = textColor,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private fun roundLabel(step: GuideStep): String {
    if (step.name.isNotBlank()) return step.name
    if (step.title.isNotBlank()) return step.title
    return "Round"
}

private fun subsectionLabel(step: GuideStep): String {
    if (step.title.isNotBlank()) return step.title
    if (step.name.isNotBlank()) return step.name
    return "Subsection"
}

private fun numberedText(step: GuideStep): String {
    if (step.content.isNotBlank()) return step.content
    if (step.title.isNotBlank()) return step.title
    if (step.name.isNotBlank()) return step.name
    return "Step"
}

private fun commentText(step: GuideStep): String {
    if (step.content.isNotBlank()) return step.content
    if (step.name.isNotBlank()) return step.name
    if (step.title.isNotBlank()) return step.title
    return "Comment"
}
