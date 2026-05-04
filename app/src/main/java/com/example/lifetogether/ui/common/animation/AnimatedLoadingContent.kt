package com.example.lifetogether.ui.common.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable

private const val ANIMATION_DURATION_MS = 650

@Composable
fun AnimatedLoadingContent(
    isLoading: Boolean,
    label: String,
    loadingContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AnimatedContent(
        targetState = isLoading,
        transitionSpec = {
            fadeIn(animationSpec = tween(ANIMATION_DURATION_MS)) togetherWith
                fadeOut(animationSpec = tween(ANIMATION_DURATION_MS))
        },
        label = label,
    ) { loading ->
        if (loading) {
            loadingContent()
        } else {
            content()
        }
    }
}
