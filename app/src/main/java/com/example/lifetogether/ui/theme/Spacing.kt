package com.example.lifetogether.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SpacingTokens(
    val xSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val xLarge: Dp = 32.dp,
    val xxLarge: Dp = 40.dp,
    val xxxLarge: Dp = 48.dp,
    val bottomInsetLarge: Dp = 80.dp,
)

val AppSpacing = SpacingTokens()

val LocalSpacing = staticCompositionLocalOf { AppSpacing }
