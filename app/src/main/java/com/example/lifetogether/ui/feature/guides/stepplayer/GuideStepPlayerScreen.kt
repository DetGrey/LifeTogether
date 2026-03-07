package com.example.lifetogether.ui.feature.guides.stepplayer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.lifetogether.ui.common.dialog.ErrorAlertDialog
import com.example.lifetogether.ui.feature.guides.stepplayer.components.GuideStepPlayerContent
import com.example.lifetogether.ui.navigation.AppNavigator
import com.example.lifetogether.ui.viewmodel.AppSessionViewModel

@Composable
fun GuideStepPlayerScreen(
    appNavigator: AppNavigator? = null,
    appSessionViewModel: AppSessionViewModel,
    guideId: String,
    guideStepPlayerViewModel: GuideStepPlayerViewModel,
) {
    val userInformation by appSessionViewModel.userInformation.collectAsState()
    val uiState by guideStepPlayerViewModel.uiState.collectAsState()

    BackHandler(enabled = appNavigator != null) {
        guideStepPlayerViewModel.flushPendingChanges()
        appNavigator?.navigateBack()
    }

    LaunchedEffect(userInformation?.familyId, userInformation?.uid, guideId) {
        val familyId = userInformation?.familyId
        val uid = userInformation?.uid
        if (!familyId.isNullOrBlank() && !uid.isNullOrBlank()) {
            guideStepPlayerViewModel.setUpGuide(familyId, uid, guideId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            guideStepPlayerViewModel.flushPendingChanges()
        }
    }

    GuideStepPlayerContent(
        uiState = uiState,
        onBack = {
            guideStepPlayerViewModel.flushPendingChanges()
            appNavigator?.navigateBack()
        },
        onPrevious = guideStepPlayerViewModel::goToPreviousStep,
        onCompleteCurrentAndGoNext = guideStepPlayerViewModel::completeCurrentAndGoNext,
        onToggleCurrentStepCompletion = guideStepPlayerViewModel::toggleCurrentStepCompletion,
    )

    if (uiState.showAlertDialog) {
        LaunchedEffect(uiState.error) {
            guideStepPlayerViewModel.dismissAlert()
        }
        ErrorAlertDialog(uiState.error)
    }
}
