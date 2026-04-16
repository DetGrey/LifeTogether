package com.example.lifetogether.ui.feature.loading

import androidx.lifecycle.ViewModel
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class LoadingViewModel @Inject constructor(
    sessionRepository: SessionRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionRepository.sessionState
}
